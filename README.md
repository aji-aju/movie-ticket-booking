# Movie Ticket Booking System

A Spring Boot service for browsing shows, **holding** seats, and **booking** them — built around the
hard part of the problem: **concurrent seat allocation with no double-booking**, plus **auto-expiring
holds**. PostgreSQL-backed, fully tested with Testcontainers (including a concurrency proof).

> DMG SDE-2 take-home. The focus is a clean, finished, well-tested core rather than maximum breadth —
> see [Scope & deliberate cuts](#scope--deliberate-cuts).

---

## The core challenge: no double-booking

Two users clicking "book seat A5" at the same instant must result in **exactly one** booking. The flow
is split into two phases so the UX is responsive *and* correct:

```
browse → HOLD (fast, optimistic) → BOOK (authoritative, locked) → pay → confirm (async notify)
                  │                          │
         held_until = now()+TTL      SELECT … FOR UPDATE
         auto-expires if abandoned   the serialization point
```

| Phase    | Strategy | Why |
|----------|----------|-----|
| **Hold** | Atomic conditional `UPDATE … WHERE status='AVAILABLE'` (compare-and-set), check `rowsAffected == n` | Lock-free and fast — holds are frequent, contention is rare, and an abandoned hold is cheap to reclaim. |
| **Book** | `SELECT … FOR UPDATE` on the held `show_seat` rows inside one `@Transactional`, re-verify still `HELD` by *this* hold and not expired, then flip to `BOOKED` | Booking is the authoritative money step; a pessimistic row lock makes the check-then-act atomic against concurrent bookers. |
| **Expiry** | `@Scheduled` sweeper releases `HELD` seats whose `held_until < now()` back to `AVAILABLE` | Abandoned holds self-heal; seats never get stuck. |
| **Retry safety** | `Idempotency-Key` (globally-unique `idempotency_key` on `booking`) | A retried submit replays the original booking instead of creating a second one. |

**Alternatives considered**

- *Optimistic locking (`@Version`) for booking* — would force the loser to retry; under real seat
  contention a pessimistic lock gives simpler, predictable semantics. (`show_seat.version` still exists
  as a JPA guard for non-locked writes.)
- *Pure CAS for booking too* — works, but the book step does several reads/writes (price, payment,
  hold conversion); a single `FOR UPDATE` boundary is clearer than threading CAS through all of them.
- *Distributed lock (Redis/ZK)* — unnecessary; the database row **is** the contended resource, so the
  lock belongs there. Avoids a second moving part.

The concurrency correctness is **proven by a test** (see [Testing](#testing)): N threads race for the
same seat → exactly one succeeds.

---

## Tech stack

- **Java 21** (builds/runs on JDK 24), **Spring Boot 3.4.1** — Web, Data JPA, Validation, Security
- **PostgreSQL 16**, schema via **Flyway** migrations (`V1`–`V7`)
- **Gradle** (wrapper 8.14), **springdoc-openapi** (Swagger UI)
- **Testcontainers** + JUnit 5 for integration & concurrency tests

---

## Run it

**Prerequisites:** Docker (for Postgres + Testcontainers) and a JDK 21+.

```bash
# 1. Start Postgres
docker-compose up -d

# 2. Run the app (Flyway applies the schema + seed data on boot)
./gradlew bootRun

# 3. Open the API docs
open http://localhost:8080/swagger-ui.html
```

### Seed data & credentials (dev only)

Seeded by Flyway (`V2`/`V4`). Passwords are all `password` (HTTP Basic).

| User    | Role     | Use for |
|---------|----------|---------|
| `admin` | ADMIN    | admin-only routes |
| `alice` | CUSTOMER | hold / book / cancel |
| `bob`   | CUSTOMER | second customer (IDOR / concurrency tests) |

Catalog: **Bengaluru → PVR → Screen 1 → "Inception"**, show id `1`, seats `A1–A5` (REGULAR ₹200) and
`B1–B5` (PREMIUM ₹300). Discount codes: `SAVE10` (10% off), `FLAT50` (₹50 off), `EXPIRED` (inactive).

> The seeded show time is `now() + 1 day` **at first migration**. If your dev DB has been running for a
> while, re-seed with `docker-compose down -v && docker-compose up -d` so the show is in the future
> (refund tiers are time-based).

### Try the happy path

```bash
# Browse (public)
curl localhost:8080/shows

# Hold seat 1 (CUSTOMER)
curl -u alice:password -X POST localhost:8080/holds \
  -H 'Content-Type: application/json' -d '{"showId":1,"showSeatIds":[1]}'

# Book it (retry-safe via Idempotency-Key)
curl -u alice:password -X POST localhost:8080/bookings \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"holdId":<HOLD_ID>,"showSeatIds":[1]}'

# A second customer booking the same seat → 409 Conflict
```

---

## API

| Method & path | Auth | Notes |
|---|---|---|
| `GET /shows[?city=]` | public | List shows (+ optional city filter) |
| `GET /shows/{id}/seats` | public | Seat map with live status |
| `POST /holds` | CUSTOMER | Hold AVAILABLE seats → **201** (409 if taken, 404 if show unknown) |
| `POST /bookings` | CUSTOMER | Confirm a hold → **201** (`Idempotency-Key` header optional) |
| `GET /bookings` | CUSTOMER | Caller's booking history (newest first) |
| `POST /bookings/{id}/cancel` | CUSTOMER | Release seats + time-based refund |

Errors are **RFC-7807 `ProblemDetail`** (`application/problem+json`): `404` not found, `409` conflict
(seat taken / already cancelled / idempotency collision), `403` forbidden (cross-user access), `400`
validation. Auth is HTTP Basic; RBAC is enforced (an ADMIN cannot hold/book, a CUSTOMER cannot reach
admin routes).

---

## Design

**Layering:** `controller → service → repository`, with DTOs at the boundary, entities inside, and a
`@RestControllerAdvice` translating exceptions to `ProblemDetail`. `open-in-view=false`.

**Domain:** `City → Theater → Screen → Show`; `Screen → Seat` (physical layout). The bookable unit is
**`ShowSeat`** = (Show × Seat) with `status: AVAILABLE → HELD → BOOKED` (and back to AVAILABLE on
expiry/cancel). `SeatHold`, `Booking`, `Payment`, `Notification`, `DiscountCode`, `AppUser(role)`.

**Strategy pattern (Open/Closed):** pricing, discounts, and refunds are each an interface with
implementations selected from an injected `List<…>` registry — a new tier/code-type/refund-policy is a
new class, no edits to the booking flow.
- *Pricing* — tier base price × 1.25 weekend surcharge.
- *Discount* — percentage / flat, resolved by `discount_code`.
- *Refund* — tiered by time-to-show: 100% (>24h), 50% (≥2h), 0% (last-minute).

**Async, non-blocking confirmation:** booking publishes a `BookingConfirmedEvent`; an
`@Async @TransactionalEventListener(AFTER_COMMIT)` records the notification on a **bounded** worker pool
— so it never blocks the booking response and never fires for a rolled-back booking.

---

## Testing

```bash
./gradlew test     # spins up Postgres via Testcontainers automatically
```

**42 tests** across unit (Strategy impls) and Testcontainers integration (real Postgres + full servlet
stack). Highlights:

- **The concurrency proof** (`BookingConcurrencyIT`): N threads book the *same* seat → asserts **exactly
  one** success, the rest rejected, and the seat is `BOOKED` once. This is the test that justifies the
  pessimistic lock.
- **`RealServerIT`** — real HTTP on a random port + Spring Security; catches live-only behavior MockMvc
  misses (PathPattern base-path matching, JDBC null-param typing).
- IDOR (cross-user hold/booking → 403), per-user idempotency, hold expiry, cancel + refund, validation.

---

## Scope & deliberate cuts

**Built:** the full browse→hold→book→pay→confirm flow, concurrency correctness + auto-expiry, RBAC,
validation + RFC-7807 errors, pricing/discounts/refunds (Strategy), cancel+refund, idempotency, async
notifications, and the test suite incl. the concurrency proof.

**Intentionally cut** (documented, not forgotten):

- **Admin CRUD UI/endpoints** — admin data is seeded; RBAC is proven. Full admin CRUD was lower-value
  than depth on the core.
- **History pagination** — fine at seed scale; the index (`idx_booking_user`) is in place for when it
  isn't. The per-booking seat lookup in history is a known N+1 (acceptable for the expected size;
  `idx_show_seat_booking` keeps each lookup an index scan).
- **Real payment gateway** — `PaymentGateway` is a mock abstraction. Note: the mock charge runs *inside*
  the booking transaction; a real network call would be moved out (outbox/saga) so it doesn't hold the
  DB connection.
- **Idempotency keys are globally unique** (not per-user namespaced) — a simplification; two users can't
  reuse the same literal key string. Tested and documented as intended.
- Out of scope by design: microservices, Kafka, OAuth/SSO, a UI, deploy/CI-CD.

---

## AI-assisted development

This was built with Claude Code. See [`docs/ai/`](docs/ai/): the execution
[`PLAN.md`](docs/ai/PLAN.md), the running [`prompts.md`](docs/ai/prompts.md) log, and a
[`AI-WORKFLOW.md`](docs/ai/AI-WORKFLOW.md) summary of how the AI was directed and reviewed.
