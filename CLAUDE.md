# CLAUDE.md

Guidance for Claude Code (and any AI assistant) working in this repository.

## What this is

A Movie Ticket Booking service (Spring Boot + Postgres). The graded core is **concurrent seat booking
with no double-allocation** and **auto-expiring holds**. Optimize for a clean, correct, well-tested core
over breadth. See `README.md` for the full design and `docs/ai/PLAN.md` for the execution plan.

## Stack & versions

- Java 21 language level (builds on JDK 24), Spring Boot 3.4.1
- Gradle wrapper 8.14 — **use `./gradlew`**, not a system Gradle/Maven
- PostgreSQL 16, schema via Flyway (`src/main/resources/db/migration`)
- Testcontainers + JUnit 5; springdoc-openapi for Swagger

## Commands

```bash
docker-compose up -d        # Postgres (required for bootRun AND tests)
./gradlew bootRun           # run; Flyway applies schema + seed on boot
./gradlew test              # unit + Testcontainers integration (auto-starts Postgres)
./gradlew build             # compile + test + jar
```

If a stale app holds port 8080: `lsof -nP -tiTCP:8080 -sTCP:LISTEN | xargs -r kill -9`
(prefer this over `pkill -f` — the forked bootRun JVM isn't reliably matched).

## Architecture & conventions

- Layered: `controller → service → repository`. DTOs (records) at the boundary; entities never leave the
  service layer. `open-in-view=false`.
- Errors: throw domain exceptions (`NotFoundException` 404, `ConflictException` 409,
  `ForbiddenException` 403); `GlobalExceptionHandler` (`@RestControllerAdvice`) maps them to RFC-7807
  `ProblemDetail`. Don't return raw error strings or `ResponseEntity` error bodies from controllers.
- Concurrency rules (do not weaken):
  - **Hold** = atomic `UPDATE … WHERE status='AVAILABLE'` + check affected rows (CAS). Lock-free.
  - **Book** = `@Lock(PESSIMISTIC_WRITE)` `SELECT … FOR UPDATE`, re-verify HELD-by-this-hold + not
    expired, then flip to BOOKED — all in one `@Transactional`.
  - Idempotency via the globally-unique `booking.idempotency_key`.
- **Bulk `@Modifying` + `clearAutomatically=true` detaches managed entities.** This already caused a
  cancel bug (the `Booking` status update was lost). If a method runs a bulk update *and* mutates a
  managed entity afterward, don't clear the context — or re-load / save explicitly.
- Extensible pricing/discount/refund: add a new `Strategy` implementation; the registry picks it up. Do
  not add `if/switch` on type into the booking flow.
- Async work runs on the bounded `taskExecutor` (`AsyncConfig`), after commit via
  `@TransactionalEventListener(AFTER_COMMIT)`. Notification failures must never break booking.

## Security routing gotcha

Spring `PathPattern "/x/**"` does **not** match the bare `/x`. Security matchers must list both the base
path and `/x/**` (see `SecurityConfig`). Verify auth changes with `RealServerIT` (real HTTP), not just
MockMvc — several auth/routing bugs were live-only.

## Testing requirements

- Every behavior change needs a test. Integration tests extend `AbstractIntegrationTest` (singleton
  Postgres container + `@BeforeEach` state reset — keep the seeded catalog intact, reset mutable rows).
- Preserve **`BookingConcurrencyIT`** (the no-double-booking proof) and **`RealServerIT`** (live HTTP).
- When adding a table that FKs to `booking`, add it to the `@BeforeEach` reset (and `ON DELETE CASCADE`
  where async writes can race the reset, as `notification` does).

## Migrations

- Schema changes are **new** Flyway files (`V8__…`), never edits to applied migrations (checksum).
- Postgres does not auto-index FK columns — add indexes for new hot read paths.

## Scope guardrails

Don't add: microservices, Kafka, real payment integration, OAuth/SSO, a UI, CI/CD, an observability
stack. If tempted to add breadth, prefer depth/tests on the core and note the cut in `README.md`.

## Commit style

One focused commit per milestone/fix, imperative subject + a short body explaining the *why*
(see git history for the pattern). Run `./gradlew test` green before committing.
