# Assertion Rules

## Avoid

```java
page.waitForTimeout(5000);   // fixed sleep — flaky and slow
```

## Prefer

- `PlaywrightAssertions.assertThat(locator).isVisible()` / `.hasText(...)` / `.isEnabled()` — retries automatically until the condition is true or times out, no manual wait needed.
- Plain TestNG `Assert.assertEquals`/`assertTrue` for values already retrieved via `.textContent()`/`.getAttribute()` — no retry needed since the value is already in hand.
- Asserting on the resulting UI state after an action (a flash message, a URL change, an element becoming visible) rather than on intermediate/implementation details.

## AI Validation

Flag:
- Any `page.waitForTimeout(...)` call.
- A missing assertion after a UI action (a test that performs a login but never checks the outcome).
- An assertion on something implementation-specific (a CSS class toggling) instead of user-visible behavior (a message appearing, a redirect happening).
