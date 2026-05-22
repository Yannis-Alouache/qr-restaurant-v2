# Engineering Standards

Always write code as a senior software engineer. Follow SOLID, clean architecture, TDD, and clean code principles for every task. Detailed references are in `.claude/rules/`.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

- No features beyond what was asked. No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

## 3. Boy Scout Rule

> "Leave the code better than you found it."

Every time you touch code:
- Improve one small thing
- Fix one naming issue
- Extract one method
- Add one missing test

When your changes create orphans, remove imports/variables/functions that YOUR changes made unused. Don't remove pre-existing dead code unless asked.

## 4. Goal-Driven Execution

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
```

## Technical Principles

All details, examples, and code snippets in `.claude/rules/`:

- **TDD**: Red-Green-Refactor. No production code without a failing test. Rule of Three before extracting.
- **SOLID**: SRP, OCP, LSP, ISP, DIP — see `solid-principles.md`
- **Clean Code**: naming conventions, object calisthenics, structure rules — see `clean-code.md`
- **Architecture**: vertical slicing, horizontal layers, dependency rule — see `architecture.md`
- **Testing**: pyramid (unit > integration > e2e), AAA pattern — see `testing.md`
- **Design patterns**: use only when they emerge from refactoring — see `design-patterns.md`

> "A little bit of duplication is 10x better than the wrong abstraction."
