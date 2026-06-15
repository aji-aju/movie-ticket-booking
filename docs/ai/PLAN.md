# Movie Ticket Booking System — Take-Home Execution Plan

## Context
DMG Companies SDE-2 take-home (48h, Spring Boot, pick 1 of 4). Goal: a **clean, finished, well-tested**
Movie Ticket Booking submission that **nails the core challenge — concurrent seat booking with no
double-allocation + auto-expiring holds** — shows strong OO/design + tests, and is **calibrated to not
over/under-deliver**. AI-assisted dev is expected and must be documented (`CLAUDE.md`, artifacts, video).

## Locked decisions
- **Stack:** Java 17+, Spring Boot 3.x — Web, Data JPA, Validation, Security (basic RBAC), Flyway (migrations).
- **DB:** PostgreSQL via `docker-compose`; **Testcontainers** for integration + concurrency tests (runs anywhere with Docker).
- **Concurrency:** pessimistic `SELECT … FOR UPDATE` on `ShowSeat` for **booking**; atomic conditional `UPDATE … WHERE status='AVAILABLE'` for **hold**; `@Transactional` boundaries; **idempotency key** on book/pay.
- **Scope:** depth-first; seed data + minimal admin; breadth only if time; document cuts in README.

## Scope tiers (calibration)
- **MUST:** browse→hold→book→pay→confirm flow · seat-concurrency correctness · auto-expiring holds · RBAC (admin/customer) · validation+errors · tests incl. the concurrency proof · README + video.
- **SHOULD:** pricing tiers + discount codes + refund policies (**Strategy**) · cancel→refund · idempotency · async (non-blocking) confirmation notifications.
- **NICE (only spare time):** booking history · full admin CRUD · seat-layout mgmt · mock payment-gateway abstraction · audit log.
- **DON'T:** microservices · Kafka/distributed queues · real payment integration · OAuth/SSO · UI · deploy/CI-CD · observability stack.

## Domain model
`City 1→N Theater 1→N Screen 1→N Show`; `Screen 1→N Seat` (layout); **`ShowSeat`** = (Show × Seat) bookable
unit with `status: AVAILABLE→HELD→BOOKED` (+ released back to AVAILABLE on expiry/cancel), `version`,
`heldUntil`, `holdId`; `SeatHold`; `Booking 1→N ShowSeat`; `PricingTier`; `DiscountCode`; `Payment`;
`RefundPolicy`/`Refund`; `User(role: ADMIN|CUSTOMER)`; `Notification`.

## Concurrency design (the core — the thing being graded)
- **Hold:** `UPDATE show_seat SET status='HELD', hold_id=?, held_until=now()+TTL WHERE id=? AND status='AVAILABLE'` → check affected rows = 1 (CAS). Lock-free, fast.
- **Book:** inside `@Transactional`: `SELECT … FOR UPDATE` the held seats → verify still `HELD` by *this* `holdId` and not expired → set `BOOKED` → create `Booking` + `Payment`. **Idempotency key** dedups retries.
- **Expiry:** `@Scheduled` sweeper releases `HELD` seats where `held_until < now()` back to `AVAILABLE`.
- README documents the alternatives considered (optimistic `@Version`, pure CAS) and why pessimistic for booking.

## API surface (key endpoints)
- Customer: `GET /shows?city=`, `GET /shows/{id}/seats`, `POST /holds`, `POST /bookings` (Idempotency-Key header), `POST /bookings/{id}/cancel`, `GET /bookings` (history).
- Admin: seed-backed; a few `POST /admin/shows`, `POST /admin/pricing-tiers`, `POST /admin/discount-codes`, `POST /admin/refund-policies`.

## Package structure
`controller / service / domain (entities) / repository / dto / strategy (pricing,discount,refund) / scheduler / config / exception`. Layered; DTOs at the boundary; `@ControllerAdvice` for errors.

## Testing plan
- **Unit:** pricing/discount/refund Strategy impls; hold + expiry logic.
- **Integration (Testcontainers + Postgres):** full booking flow, RBAC enforcement, validation errors.
- **THE concurrency test:** N threads attempt to book the *same* seat → assert **exactly 1 success**, rest rejected, and DB shows the seat `BOOKED` once. Run it once with the lock removed to show it *would* double-book.

## Submission checklist
- GitHub repo, **frequent incremental commits** (one per milestone below).
- **README:** assumptions · design decisions · concurrency approach + alternatives · scope cuts + reasons · run instructions.
- **CLAUDE.md:** project context · architecture · conventions · test requirements · scope guardrails · commands (seed from this plan).
- Keep **AI artifacts** (prompts/raw files) + "skills".
- **Loom video ≤10 min:** approach · stack reasoning · AI workflow · testing (demo the concurrency proof).

## AI artifacts & raw-file tracking (DO from milestone 1 — don't reconstruct at the end)
- `docs/ai/PLAN.md` ← this plan; `CLAUDE.md` at repo root (evolving).
- `docs/ai/prompts.md` ← running log: append key prompts + the decision behind each, per milestone.
- Curate relevant **Claude Code session transcripts** (`~/.claude/projects/…`) into the repo before submission.
- `docs/ai/AI-WORKFLOW.md` ← short summary drafted at the end from the running log (tools, how AI was directed, review process).
- Commit these alongside code each milestone; keep them authentic (real prompts/sessions, no after-the-fact fabrication).

## Build order (commit after each; stop anytime with a working tested core — MUST tier first)
1. Skeleton + `docker-compose` Postgres + Flyway migrations + entities + seed data
2. Auth/RBAC (admin/customer)
3. Browse shows/seats (read flows)
4. **Hold** (atomic CAS) + `@Scheduled` expiry sweeper
5. **Book** (pessimistic lock) + payment + idempotency + **the concurrency test**
6. Pricing tiers + discount codes + refund policies (Strategy)
7. Cancel + refund
8. Async (non-blocking) confirmation notification
9. Polish: validation, error handling, README, CLAUDE.md, record video

## Verification
- `docker-compose up -d` → `./mvnw spring-boot:run` → exercise flows via curl/Postman (hold → book → confirm; second booker on same seat → 409).
- `./mvnw test` → unit + Testcontainers integration + concurrency test all green.
