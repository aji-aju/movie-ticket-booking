# Skills Used During Development

This document records the concrete AI-assisted development skills and techniques applied
while building this Movie Ticket Booking service. The assignment expects AI use and asks
that it be documented honestly. Everything below is tied to evidence in this repo:
`docs/ai/PLAN.md`, `docs/ai/prompts.md`, `docs/ai/AI-WORKFLOW.md`, the root `CLAUDE.md`,
the test suite, and the git history.

> Tooling: **Claude Code** (CLI agent) was the primary pair — reading/writing files,
> running Gradle and `curl`, driving Docker/Postgres, and committing. The model wrote the
> code; the author owned every decision: scope, design tradeoffs, what to cut, and
> acceptance of each change. No personal/custom Claude Code skill files were installed or
> used (`~/.claude/skills/` does not exist); the techniques below are workflow practices,
> not packaged skills.

## 1. Plan-first scoping with MUST / SHOULD / NICE / DON'T tiers

The first interaction was scoping, not coding: read the assignment, agree on the
evaluation criteria, and lock a tiered scope so the build neither over- nor
under-delivered. The result is the explicit tier list in `docs/ai/PLAN.md`:

- **MUST** — browse to hold to book to pay to confirm flow; seat-concurrency correctness;
  auto-expiring holds; RBAC; validation/errors; tests including the concurrency proof; README.
- **SHOULD** — pricing tiers + discount codes + refund policies (Strategy); cancel/refund;
  idempotency; async confirmation notification.
- **NICE** — booking history, full admin CRUD, seat-layout management, mock payment gateway,
  audit log (only if spare time).
- **DON'T** — microservices, Kafka, real payment integration, OAuth/SSO, a UI, CI/CD, an
  observability stack.

Decision recorded up front: depth on the concurrency core beats breadth, and every cut is
documented. The matching "Scope guardrails" section in `CLAUDE.md` keeps the AI from
drifting into scope creep on later turns.

## 2. One commit per milestone

`PLAN.md` defines a depth-first build order (M1–M9), and the git history mirrors it
one-to-one — each milestone is a self-contained unit (implement → test → run for real →
commit). Representative commits:

```
0be2701 M1: project skeleton, Postgres + Flyway schema, JPA entities, seed, smoke test
7cdc89f M2: Spring Security auth + RBAC
9986238 M3: browse flows + integration tests
d970bc9 M4: seat holds (atomic CAS) + scheduled expiry
b256ed0 M5: booking (pessimistic lock) + payment + idempotency + concurrency proof
59c5795 M6: pricing tiers + discount codes + refund policies (Strategy)
91ce5ba M7: cancel booking + time-based refund
3b1c0b8 M8: async (non-blocking) booking confirmation notification
627a6aa M9: polish -- validation, REST status codes, indexes, docs
```

Commit style (documented in `CLAUDE.md`): one focused commit per milestone/fix, imperative
subject plus a short body explaining the *why*, with `./gradlew test` green before each commit.

## 3. Tests as the contract

Nothing was considered "done" until `./gradlew test` was green. Tests were written
alongside the code and used as the acceptance gate, not as an afterthought. The suite spans
unit and Testcontainers-backed integration tests, including:

- `BookingConcurrencyIT` — the no-double-booking proof: N threads attempt to book the same
  seat; assert exactly one success and the seat ends `BOOKED` once.
- `RealServerIT` — real-HTTP checks (see technique 4).
- Flow/behavior coverage: `HoldIT`, `BookingIT`, `CancelIT`, `DiscountBookingIT`,
  `NotificationIT`, `ShowControllerIT`, plus `PricingStrategyTest` / `RefundStrategyTest`.

This habit caught real bugs early — e.g. the double-cancel defect (a second cancel returned
200 instead of 409) surfaced through a test. `CLAUDE.md` codifies the rule: *every behavior
change needs a test*, and `BookingConcurrencyIT` / `RealServerIT` must be preserved.

## 4. "Run it for real" — live verification beyond MockMvc

A recurring directive was **"run it for real"**: every flow was exercised against an
actually-running server via `curl` and Swagger, not just MockMvc. This was the
highest-value habit of the build — it caught defects MockMvc could not see, because they
were live-only:

- an unknown-show FK error returning 500 where it should be 404,
- a Spring Security `PathPattern` base-path mismatch (`"/x/**"` does not match bare `/x`),
- JDBC null-typing in `lower(:city)` when the `city` filter was absent,
- an RBAC enforcement gap.

These were made permanent by adding `RealServerIT` (real HTTP) as a standing guard, and the
gotcha is written into `CLAUDE.md` so the lesson persists across sessions.

## 5. Adversarial self-audit (IDOR / idempotency / unsafe patterns)

A dedicated review pass deliberately attacked the system looking for authorization and
race-condition flaws, rather than only confirming the happy path. Confirmed findings were
fixed and then turned into tests/rules:

- **IDOR** — cross-user access to another user's hold/booking now returns 403
  (commit `4dcfadb security: fix booking IDOR + scope idempotency per-user`).
- **Idempotency scoping** — the idempotency key was scoped per user via a composite-unique
  `(user_id, idempotency_key)` constraint, with a later hardening pass adding a per-user
  idempotency guard and a cancel-row lock (commit `646ee82 harden concurrency: per-user
  idempotency, cancel row lock, +2 proofs`).

The audit treated security as something to be proven, not assumed.

## 6. Root-cause-over-patch, captured as durable `CLAUDE.md` rules

When a defect appeared, the fix waited until the underlying cause was understood — then the
finding was written into `CLAUDE.md` so it could never silently regress. The clearest
example: the double-cancel bug was traced not to the cancel logic but to JPA mechanics —
`@Modifying(clearAutomatically=true)` was detaching the managed `Booking` entity, so its
status update was lost. The fix was to not clear the persistence context for that bulk
update, and the lesson became a standing rule:

> **Bulk `@Modifying` + `clearAutomatically=true` detaches managed entities.** If a method
> runs a bulk update *and* mutates a managed entity afterward, don't clear the context — or
> re-load/save explicitly.

`CLAUDE.md` accumulates this kind of hard-won, project-specific knowledge (concurrency
rules that must not be weakened, the security routing gotcha, migration/indexing rules)
so the context carries forward instead of being relearned each session.

## 7. Design-pattern discipline enforced through the AI

The AI was directed to keep pricing, discounts, and refunds extensible via the **Strategy**
pattern (Open/Closed): three interfaces with a registry built from an injected `List<…>`,
and explicitly **no `if`/`switch` on type** inside the booking flow. This is reflected in
the dedicated `strategy` package and is locked in as a `CLAUDE.md` convention so future
changes add a new strategy implementation rather than branching logic into the core.

## 8. Persistent AI context engineering via `CLAUDE.md`

Rather than re-explaining the project each session, hard constraints were front-loaded into
`CLAUDE.md` — stack/versions, exact commands, layering (`controller → service →
repository`, DTOs at the boundary), RFC-7807 error shape, the (non-weakenable) concurrency
design, testing requirements, migration rules, and the scope guardrails. This made the AI's
output consistent across milestones and prevented scope drift.

## 9. Authentic AI artifact tracking (from milestone 1, not reconstructed)

Per `PLAN.md`, the AI artifacts were written *as the work happened* and committed alongside
the code each milestone, not fabricated at the end:

- `docs/ai/PLAN.md` — the execution plan agreed before coding.
- `docs/ai/prompts.md` — a running log of the intent-shaping prompts and the decision behind
  each, kept per milestone (routine "fix this compile error" turns omitted).
- `docs/ai/AI-WORKFLOW.md` — the end-of-build summary of tooling, direction, and review.
- This `docs/ai/SKILLS.md`.

## 10. Safe handling of credentials and access

A constraint set by the author and honored throughout: nothing touching secrets was handed
to the AI. GitHub access was done via interactive `gh auth login` (never a token pasted into
chat), and the repository was kept private during development and made public for submission. Scope and cuts were also kept as author-only
decisions.

## What the AI was *not* allowed to decide

- Scope and cuts (kept deliberately narrow; documented in README/PLAN).
- Anything involving credentials/secrets.

---

*Honesty note: a multi-agent / sub-agent orchestration workflow is **not** claimed here —
this build used a single Claude Code agent as the primary pair, which is what the repo's
artifacts actually document. The techniques above are the ones with concrete evidence in
this repository.*
