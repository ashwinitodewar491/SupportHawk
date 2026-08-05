# Code Review Checklist — Playwright UI (Java + TestNG)

Run through this before finalizing generated code, and whenever asked to review existing UI test code in this project.

## Structure
- [ ] Extends `BasePage`.
- [ ] All locators live in a page object — none inline in the test class.
- [ ] One page object per screen; one test class per feature/flow.

## Locators
- [ ] `getBy*` role/label/text preferred; CSS/id only when documented as a last resort.
- [ ] No XPath.
- [ ] No `nth-child()`/`.nth()` unless genuinely unavoidable.
- [ ] No dynamic/generated CSS class selectors.

## Waits & Assertions
- [ ] No `page.waitForTimeout(...)`.
- [ ] `PlaywrightAssertions.assertThat(...)` used for visibility/state checks.
- [ ] Every action that changes UI state has a following assertion.
- [ ] Assertions target user-visible behavior, not implementation details.

## TestNG conventions
- [ ] Every `@Test` has `groups = {...}` including `ui`.
- [ ] `smoke` tests are also tagged `regression`.
- [ ] No `@BeforeClass`/`@AfterClass` used for browser lifecycle (that's `BasePage`'s job).
- [ ] Descriptive `description` attribute on every `@Test`.

## Style
- [ ] No comments that just restate what the code does.
- [ ] Method names follow `test{WhatIsVerified}` / page objects follow `{Screen}Page`.
