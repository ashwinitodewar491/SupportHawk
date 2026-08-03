# Code Quality

## Naming

- Page object: `{Screen}Page`. Test class: `{Feature}Test`. Test method: `test{WhatIsVerified}`.
- Locator constant/field names describe the element, not its tag (`usernameInput`, not `input1`).

## Method size and complexity

- A page-object method should do one user-visible action or one read (`login(...)`, `getFlashMessageText()`) — not a whole multi-screen flow. Compose multi-screen flows in the test class, not inside a single page-object method.
- A test method should read as: navigate → act → assert. If it needs several unrelated assertion groups, split it into separate `@Test` methods.

## Duplication

- If the same navigation sequence appears in 3+ test methods within a class, extract it into a private `navigateTo{Screen}Page()` helper (see `LoginTest`).
- If two page objects share several locators/methods (e.g. a shared header/nav present on every screen), extract a common base or a `HeaderComponent` page object rather than duplicating those locators in every page.
- Don't re-implement a wait/assertion pattern that Playwright already provides (`PlaywrightAssertions.assertThat(...)`) — use the library instead of hand-rolled polling.

## Dead code

- Remove commented-out code, unused imports, and unused locator fields before finalizing.
- Remove a page object entirely if the screen it represents is no longer tested — don't leave orphaned page objects.

## AI Review Checklist

- Does every page-object method do one thing?
- Is there duplicated navigation/setup logic that should be a shared helper?
- Are there unused locators, imports, or orphaned page objects?
- Could an existing Playwright assertion/wait replace hand-rolled polling logic?
