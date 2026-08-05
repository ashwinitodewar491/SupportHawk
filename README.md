# SupportHawk — Playwright UI (Java + TestNG)

UI test automation using Playwright's Java bindings. Example tests run against [the-internet.herokuapp.com](https://the-internet.herokuapp.com), a long-standing, stable, purpose-built QA practice site.

Conventions adapted from [QaBoilerPlate playwright-ui-testng](https://github.com/ashwinitodewar491/QaBoilerPlate/tree/master/playwright-ui-testng).

## Prerequisites

- Java 21
- Maven 3.8+

## Run

```bash
mvn clean test                    # headless (default)
mvn clean test -Dheadless=false   # headed, to watch it run
mvn clean test -DtestGroups=smoke # smoke group only
mvn test -Dtest=LoginTest         # a single class
```

## Project Structure

```
src/test/java/com/supporthawk/
├── config/AppConfig.java      # base URL + headless flag
├── base/BasePage.java         # @BeforeMethod/@AfterMethod — browser lifecycle
├── listeners/TestListener.java # wires TestNG results into the Extent HTML report
├── pages/LoginPage.java       # example page object
├── utils/ScreenshotUtil.java  # failure screenshots
└── tests/LoginTest.java       # example test class
```

## Reports, Quality, Security

- **Test report** — every `mvn test` run generates `target/extent-report/index.html` automatically (open it directly in a browser).
- **Failure artifacts** — screenshots under `target/screenshots/`, failure videos under `target/videos/`.
- **Code quality / security guidance** — enforced by the AI assistant during generation/review; see `.claude/skills/playwright-ui-automation/code-quality.md` and `security.md`.
- **Dependency vulnerability scan** — `mvn org.owasp:dependency-check-maven:check` (not part of the default `mvn test` lifecycle; run it explicitly).

## AI-Assisted Test Generation

Paste a page URL or describe a UI flow, and Claude Code / Cursor will follow this project's conventions automatically. See [`skills.md`](skills.md) for the full guide, or the detailed docs under [`.claude/skills/playwright-ui-automation/`](.claude/skills/playwright-ui-automation/).
