---
name: playwright-ui-automation
description: Generates and reviews Java + TestNG UI tests using Playwright for this project. Use when the user pastes a webpage URL, describes a UI flow to automate (login, form fill, navigation), or asks to write/review a UI test.
---

# Playwright UI Automation (Java + TestNG)

Read [`skills.md`](../../../skills.md) at the project root first — it has the full project structure, naming conventions, and step-by-step guide for adding a new test. This file covers the rules Claude must actively enforce.

## Before generating code

1. Check `src/test/java/com/supporthawk/pages/` for an existing page object for this screen before creating a new one.
2. Never put a locator directly in a test class — it goes in a page object.
3. Inspect the target page's DOM (or ask for a snippet) before guessing selectors — don't invent a `data-testid` that doesn't exist.

## Rules to enforce (see [`dos-and-donts.md`](dos-and-donts.md) for the full list)

- **Locators**: `getBy*` role/text/label first, `data-testid` next, id/CSS only as a documented last resort. See [`locator-strategy.md`](locator-strategy.md).
- **Waits**: never `page.waitForTimeout(...)` — rely on Playwright's auto-waiting or `PlaywrightAssertions.assertThat(...)`. See [`assertion-rules.md`](assertion-rules.md).
- **Groups**: every `@Test` has `groups = {...}` including `ui`, and `smoke` tests are also tagged `regression`.
- **No hardcoded hosts** — always `AppConfig.BASE_URL`.
- **Code quality**: naming, method size, duplication, dead code. See [`code-quality.md`](code-quality.md).
- **Security**: no hardcoded credentials, careful with session/state handling, dependency vulnerability awareness. See [`security.md`](security.md).

## Before finalizing or when asked to review

Check the generated/existing code against [`code-review-checklist.md`](code-review-checklist.md), [`code-quality.md`](code-quality.md), and [`security.md`](security.md). Report findings inline (file, line, what's wrong, suggested fix).

## Examples

- [`examples/good-test.java`](examples/good-test.java) — follows every convention above.
- [`examples/bad-test.java`](examples/bad-test.java) — the same test with common violations, annotated with why each is a problem.
