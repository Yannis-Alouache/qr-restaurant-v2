# Engineering Standards

Write code like a senior engineer who respects this codebase: calibrate process to task size, favor simplicity, match existing patterns over imposing theory.

## Calibrate process to task size

Don't apply heavy ceremony to light work.

| Task | Process |
|------|---------|
| **Trivial** (typo, rename, constant, formatting, pure log/text) | Just do it. Run the relevant existing tests. No new test, no refactor. |
| **Bug / behavior change** | Write a failing test that reproduces it, then fix. |
| **New feature / non-trivial logic** | Full TDD (red → green → refactor). |
| **Refactor** | Behavior unchanged. Tests green before and after. |

Open a deep-reference doc only when the task actually needs it — not by default.

## Core principles (always apply)

- **Read before write.** Understand existing code and patterns first; match them.
- **Simplicity first.** No features, abstractions, or "flexibility" beyond what was asked. If 50 lines replace 200, rewrite.
- **Be explicit.** State assumptions. When ambiguous, ask or present options instead of guessing silently. Push back when a simpler approach exists.
- **Boy scout, in proportion.** Leave code better than you found it, scaled to the task. Remove only the orphans *your* changes create — not pre-existing dead code.
- **Make it verifiable.** Turn the task into a check: "add validation" → invalid-input tests pass; "fix bug" → reproducing test passes. For multi-step work, state a short plan with a verify step each.

> A little duplication is 10× better than the wrong abstraction. Wait for the rule of three before extracting.

## Project map

Monorepo `qr-restaurant-v2` — three apps + supporting dirs.

| Path | Stack | Notes |
|------|-------|-------|
| `admin/` | Angular 21 + Tailwind | Admin panel. UI rules: `admin/DESIGN.ADMIN.md` |
| `client/` | Angular 21 + Tailwind | Customer-facing. UI rules: `client/DESIGN.CLIENT.{THEME}.md` (current theme: `CHAUD`) |
| `api/` | Spring Boot 3 + Java 21 (Maven) | REST + WebSocket/STOMP, Security (JWT), JPA, Validation, PostgreSQL, Flyway migrations, Stripe. API requests: `api/bruno/` |
| `e2e/` | Playwright | Config: `playwright.config.ts` |
| `docker/` | Docker | Local infra |

**Commands:** `npm run reset-db` · `npm run test:e2e` / `test:e2e:headed` (root) · per app: `npm test` / `npm start` (`admin/`, `client/`), `mvn test` / `mvn spring-boot:run` (`api/`).

**UI rule:** admin work → read `admin/DESIGN.ADMIN.md` first; client work → read `client/DESIGN.CLIENT.{THEME}.md` first.

## Deep references (read on demand — not loaded automatically)

Open the relevant doc(s) only when doing that kind of work:

- **Writing code / design:** `docs/standards/solid-principles.md`, `clean-code.md`, `object-design.md`, `code-smells.md`, `complexity.md`
- **Testing:** `docs/standards/tdd.md`, `testing.md`
- **Architecture & patterns:** `docs/standards/architecture.md`, `design-patterns.md`
