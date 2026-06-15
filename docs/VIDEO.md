# Walkthrough video — outline (≤10 min)

A tight script for the Loom. Goal: show I understand the *why*, not just demo clicks.

## 0:00 — Intro & framing (45s)
- One line on the problem: browse → hold → book, where the hard part is **no double-booking under
  concurrency** + **auto-expiring holds**.
- The thesis: I scoped deliberately — a clean, finished, tested core over broad-but-shallow. Point at
  the MUST/SHOULD/NICE/DON'T tiers in `PLAN.md`.

## 0:45 — Stack & why (45s)
- Spring Boot + Postgres + Flyway + Testcontainers. Why Postgres: the contended resource is a DB row, so
  the lock belongs in the DB — no Redis/distributed lock needed.

## 1:30 — Architecture tour (1.5 min)
- Layering controller → service → repository, DTOs at the edge, RFC-7807 errors via one advice.
- Domain: `ShowSeat` (Show × Seat) is the bookable, lockable unit; `AVAILABLE → HELD → BOOKED`.
- Strategy pattern for pricing/discount/refund (Open/Closed) — show the registry, not an `if/switch`.

## 3:00 — The core: concurrency design (2 min) ⭐
- **Hold** = atomic CAS (`UPDATE … WHERE status='AVAILABLE'`), lock-free.
- **Book** = `SELECT … FOR UPDATE` + re-verify + flip, in one transaction — the serialization point.
- **Expiry** = `@Scheduled` sweeper. **Idempotency-Key** for retry safety.
- Mention alternatives considered (optimistic, pure CAS, distributed lock) and why pessimistic for book.

## 5:00 — Live demo (2 min)
- Swagger → browse, hold seat, book (Authorize as alice). Show the `201`.
- Second customer books the same seat → **409**. Hold then let it expire → seat returns to AVAILABLE.
- Cancel → time-based refund. Show the async confirmation landing on the `notify-` thread in logs.

## 7:00 — Testing & the concurrency proof (1.5 min) ⭐
- `./gradlew test` → 42 green. Open `BookingConcurrencyIT`: N threads, same seat, assert exactly one
  success. This is the test that earns the pessimistic lock.
- Mention `RealServerIT` (real HTTP caught live-only bugs MockMvc missed).

## 8:30 — AI workflow (1 min)
- Plan-first, milestone-per-commit, "run it for real," tests-as-contract, adversarial self-audit.
- Point at `docs/ai/` (PLAN, prompts, workflow) and `CLAUDE.md`.

## 9:30 — Scope cuts & close (30s)
- Name the deliberate cuts (admin CRUD, pagination, real gateway) and where they're documented.
- Close: correct, tested, finished core; everything else is a documented, intentional decision.
