# Dos and Don'ts — Playwright UI (Java + TestNG)

## Do

- Put every locator in a page object, never in a test class.
- Prefer `page.getByRole(...)`, `getByLabel(...)`, `getByText(...)` over CSS/XPath.
- Use `[data-testid="..."]` when role/label/text locators aren't specific enough and the element has one.
- Use `PlaywrightAssertions.assertThat(locator)` for visibility/state — it retries automatically.
- Tag every test with `groups = {...}` including `ui`.
- Keep one page object per screen, one test class per feature/flow.
- Flag with a comment when a target page has no stable locator available at all (see `LoginPage`'s note on the missing `data-testid`s on its practice-site target).

## Don't

- Don't use Selenium, Cypress, Puppeteer, or any browser-automation library other than Playwright.
- Don't use JUnit annotations — TestNG only.
- Don't use `page.waitForTimeout(...)` (a fixed sleep) for anything — it's flaky and slow. Use a real wait condition instead.
- Don't use `nth-child`/`nth()` selectors as a first choice — only when there's genuinely no other way to disambiguate.
- Don't hardcode a host or full URL in a page object — always `AppConfig.BASE_URL`.
- Don't put setup/teardown logic in `@BeforeClass`/`@AfterClass` — `BasePage`'s `@BeforeMethod`/`@AfterMethod` already handle browser lifecycle.
- Don't add a comment that just restates what the code does.
