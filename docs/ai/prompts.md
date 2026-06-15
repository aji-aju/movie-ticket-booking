# Prompt log

A running log of the key prompts that steered this build and the decision behind each, kept per
milestone (authentic — written as the work happened, not reconstructed at the end). Routine
"fix this compile error" turns are omitted; the intent-shaping prompts are what's recorded.

## Scoping (before any code)

- **"Read the assignment thoroughly and scope it; let's discuss the evaluation criteria first so we
  don't over- or under-deliver."** → Produced the tiered scope (MUST/SHOULD/NICE/DON'T) in `PLAN.md`.
  Decision: depth on the concurrency core beats breadth; document every cut.
- **"Seed data + minimal admin first; breadth only if time."** → Locked a depth-first build order.

## M1–M3 — skeleton, auth, reads

- "Scaffold: docker-compose Postgres + Flyway + entities + seed data." → `ShowSeat` chosen as the
  bookable unit (the row we lock); `@Version` added as a guard.
- "Auth + RBAC (admin/customer)." → HTTP Basic, stateless, RBAC by role.
- "Browse shows/seats." → read endpoints; split `findAllWithDetails` / `findByCityWithDetails` after a
  live `lower(null)` SQL error (filter must not be applied when `city` is absent).

## M4–M5 — the core (hold, book, concurrency)

- "Hold via atomic CAS + a scheduled expiry sweeper." → `UPDATE … WHERE status='AVAILABLE'` + affected-
  rows check; `@Scheduled` releases expired holds.
- "Book with a pessimistic lock + payment + idempotency, **and the concurrency test**." → `SELECT … FOR
  UPDATE`; N-thread test asserting exactly one success. Decision recorded: pessimistic over optimistic
  for the authoritative step.
- **"Run it for real."** (recurring) → Manual `curl`/Swagger testing surfaced several bugs MockMvc
  missed (unknown-show FK 500 → 404; security PathPattern base-path matching; RBAC gap). Led to adding
  `RealServerIT` (real HTTP) as a permanent guard.

## M6 — pricing / discounts / refunds

- "Pricing tiers + discount codes + refund policies as **Strategy**, Open/Closed." → Three interfaces,
  registry-by-type from an injected `List<…>`; no `if/switch` in the booking flow.

## M7 — cancel + refund

- "Cancel + refund." → Time-tiered refund. **Bug found via test + live:** double-cancel returned 200,
  not 409. Root cause prompt: *"why does the booking status not persist?"* → `@Modifying
  (clearAutomatically=true)` was detaching the managed `Booking`. Fix: don't clear the context for that
  bulk update. Captured as a standing rule in `CLAUDE.md`.

## M8 — async notification

- "Async (non-blocking) confirmation." → Decision: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`
  on a **bounded** pool, so the notification fires only after commit and never blocks the response.
  Verified live by the worker thread name in the logs.

## M9 — polish

- "Apply the IDOR and idempotency fixes from the audit." → Cross-user hold/booking now 403; idempotency
  scoped per-user with a global-unique key backstop.
- "Add Swagger with Authorize + enrichment." → springdoc + HTTP-Basic security scheme.
- "Resume the feature build" → finished M6–M8, then polish: input bounds (`@Positive`/`@Size`),
  `201 Created` on POST creates, lookup indexes (`V7`), README + this AI documentation.

## Security/process constraints the user set

- GitHub access **without exposing tokens** → used interactive `gh auth login`.
- **Private** repo.
- The user stopped their own local Postgres (a Homebrew `postgresql@14` was shadowing `:5432`).
