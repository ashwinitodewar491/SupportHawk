# Security

## Credentials in tests

- Never hardcode a real user's password/credentials in a test class. `LoginTest` uses `the-internet.herokuapp.com`'s publicly documented practice credentials (`tomsmith` / `SuperSecretPassword!`) — that's fine because they're the site's own intentionally-public test account, not a real credential. For a real target app, read credentials from environment variables, never a literal.
- Don't commit a `.env` file or any file containing a real credential — check `.gitignore` covers this before adding one.

## Session/state handling

- Don't reuse a `BrowserContext` (and therefore cookies/session storage) across tests unless the test is deliberately verifying session persistence — `BasePage` already creates a fresh context per test, keep it that way so tests can't leak auth state into each other.
- Don't log full request/response bodies or storage state to console in a way that could leak a real session token in CI output.

## Dependency vulnerabilities

- Run `mvn org.owasp:dependency-check-maven:check` periodically (see project `README.md`) to check for known CVEs in dependencies. Not bound to the default `mvn test` lifecycle — run it explicitly.
- Prefer an already-used dependency over introducing a new library for the same purpose.

## AI Review Checklist

- Any hardcoded real credential (not a documented public test account)?
- Is a `.env`-style file with real secrets about to be committed?
- Does a test share browser state (cookies/storage) across test methods in a way that isn't intentional?
- Is a new dependency actually necessary?
