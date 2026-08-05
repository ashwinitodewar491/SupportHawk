# Playwright UI (Java + TestNG) — AI Skills

This file teaches AI coding assistants (Claude Code, Cursor, Copilot) how to generate and review UI tests for this project. Follow these patterns exactly — do not introduce new frameworks or abstractions.

## Project Stack

- **Language:** Java 21
- **Test framework:** TestNG 7.x
- **Browser automation:** Playwright Java (Chromium via `BasePage`)
- **Assertions:** TestNG `Assert` + Playwright's `PlaywrightAssertions.assertThat(...)` for auto-waiting UI-state checks
- **Build:** Maven (`mvn clean test`)

## Project Structure

```
src/test/java/com/supporthawk/
├── config/
│   └── AppConfig.java     # Base URL + headless flag
├── base/
│   └── BasePage.java       # TestNG @BeforeMethod/@AfterMethod — browser lifecycle
├── pages/
│   └── {Screen}Page.java   # One page object per screen/flow
└── tests/
    └── {Feature}Test.java  # One file per feature/flow
```

## Naming Conventions

| What | Pattern | Example |
|---|---|---|
| Page object | `{Screen}Page` | `LoginPage` |
| Test class | `{Feature}Test` | `LoginTest` |
| Test method | `test{WhatIsVerified}` | `testLoginWithValidCredentials` |
| Locator constant | `SCREAMING_SNAKE_CASE` | `USERNAME_INPUT` |

## Adding a New UI Test — Step by Step

1. **Create (or extend) a page object** in `pages/` — every locator lives there, never in a test class.
2. **Create the test class** extending `BasePage`, with a private `navigateTo{Screen}Page()` helper if navigation takes more than one line.
3. **Add `@Test` methods** with groups (see below): happy path first, then negative/edge cases.

```java
package com.supporthawk.tests;

public class {Feature}Test extends BasePage {

    private {Screen}Page navigateTo{Screen}Page() {
        {Screen}Page page = new {Screen}Page(this.page);
        page.navigate(AppConfig.BASE_URL);
        return page;
    }

    @Test(groups = {"smoke", "regression", "ui"}, description = "Verify {what this checks}")
    public void test{Feature}() {
        {Screen}Page screenPage = navigateTo{Screen}Page();

        screenPage.{action}();

        Assert.assertTrue({condition}, "{failure message}");
    }
}
```

4. **Add the class to `testng.xml`.**

## Locator Strategy

See [`.claude/skills/playwright-ui-automation/locator-strategy.md`](.claude/skills/playwright-ui-automation/locator-strategy.md). In short: prefer Playwright's `getBy*` role/text/label locators; fall back to `data-testid` via `page.locator("[data-testid=...]")`; only fall back further to id/CSS selectors when the target site genuinely has neither (flag this with a comment when it happens, same as `LoginPage` does for this example target).

## Assertion Rules

See [`.claude/skills/playwright-ui-automation/assertion-rules.md`](.claude/skills/playwright-ui-automation/assertion-rules.md). In short: rely on Playwright's built-in auto-waiting — never a manual `page.waitForTimeout(...)` sleep. Use `PlaywrightAssertions.assertThat(locator)` for visibility/state checks (it retries automatically), and plain TestNG `Assert` for value/text comparisons already retrieved.

## What NOT to Do

- Do not use Selenium, Cypress, or any other browser-automation library.
- Do not use JUnit — only TestNG.
- Do not put locators directly in a test class — always through a page object.
- Do not use `page.waitForTimeout(...)` (a fixed sleep) — rely on Playwright's auto-waiting or an explicit `waitFor` on a real condition.
- Do not modify `BasePage.java` for individual test needs.
- Do not add comments that describe what the code does — only add a comment for a non-obvious constraint (see `LoginPage`'s comment about the missing `data-testid` attributes on this practice site).

## Code Review

Before finalizing generated code, or when asked to review existing test code in this project, check it against [`.claude/skills/playwright-ui-automation/code-review-checklist.md`](.claude/skills/playwright-ui-automation/code-review-checklist.md).

## Running Tests

```bash
mvn clean test                                  # headless (default)
mvn clean test -Dheadless=false                  # headed, to watch it run
mvn clean test -DtestGroups=smoke                # smoke group only
mvn test -Dtest=LoginTest                        # single class
```
