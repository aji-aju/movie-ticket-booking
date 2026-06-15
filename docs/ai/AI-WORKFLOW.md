# AI workflow

How this project was built with AI assistance — the tools, how the AI was directed, and how its output
was reviewed. (AI use was expected for this take-home; this documents it honestly.)

## Tooling

- **Claude Code** (CLI agent) as the primary pair — reading/writing files, running Gradle and `curl`,
  driving Postgres/Docker, and committing.
- The model wrote the code; **I (the author) owned the decisions** — scope, design tradeoffs, what to
  cut, and acceptance of every change.

## How the AI was directed

1. **Plan before code.** First turn was scoping, not coding: read the assignment, agree
   MUST/SHOULD/NICE/DON'T tiers, and lock a depth-first build order (`PLAN.md`). This kept the build
   calibrated — finished core over half-finished breadth.
2. **Milestone by milestone, one commit each.** Each milestone (M1–M9) was a self-contained unit:
   implement → test → run for real → commit. The git history mirrors the plan's build order.
3. **Explicit constraints up front.** Concurrency rules, error-shape (RFC-7807), layering, and "no
   over-delivery" were stated as guardrails so the AI didn't drift into scope creep — these now live in
   `CLAUDE.md` so the context persists.
4. **"Run it for real."** Beyond unit/MockMvc tests, every flow was exercised against a real server
   (`curl` + Swagger). This was the highest-value habit — it caught bugs MockMvc could not.

## How output was reviewed

- **Tests as the contract.** Nothing was "done" until `./gradlew test` was green, including the
  concurrency proof. Several bugs were caught by tests written alongside the code (e.g. double-cancel).
- **Live verification.** Real-HTTP checks caught live-only defects: a Spring Security PathPattern
  base-path mismatch, JDBC null-typing in `lower(:city)`, an FK 500 that should have been a 404, and an
  RBAC gap. `RealServerIT` was added so these stay caught.
- **Adversarial self-audit.** A dedicated pass looked for IDOR, idempotency races, and unsafe Spring
  patterns; confirmed findings (cross-user IDOR, idempotency scoping) were fixed and turned into tests.
- **Root-cause over patch.** When the cancel double-spend appeared, the fix waited until the real cause
  (`clearAutomatically` detaching the entity) was understood — then captured as a rule in `CLAUDE.md`.

## What the AI was *not* allowed to decide

- Scope and cuts (kept deliberately narrow).
- Anything touching credentials/secrets — GitHub auth was done via interactive `gh auth login`, never a
  token in chat; the repo was private during development and is public for submission.

## Artifacts in this folder

- `PLAN.md` — the execution plan agreed before coding.
- `prompts.md` — running log of the intent-shaping prompts and the decision behind each.
- `AI-WORKFLOW.md` — this summary.
