# Locator Strategy

## Preferred Order

1. `page.getByRole(AriaRole.X, new Page.GetByRoleOptions().setName("..."))` — matches how users and assistive technology find elements.
2. `page.getByLabel("...")` — form inputs with an associated label.
3. `page.getByText("...")` — visible, stable text content.
4. `page.locator("[data-testid='...']")` — when role/label/text aren't specific enough and the app exposes test ids.
5. A stable `id`/CSS attribute selector — only when none of the above are available (common on older sites that predate `data-testid` conventions; document why with a comment, as `LoginPage` does).

## Avoid

- XPath.
- `nth-child()` / `.nth()` as a first choice — order-dependent and breaks the moment the DOM reorders.
- Deep parent-chain selectors (`div > div > div > button`).
- Dynamic/generated CSS classes (`.css-1a2b3c`) — these are usually build-tool-generated and change on every deploy.

## AI Review Checklist

- Is the locator resilient to a DOM reorder or a class-name change?
- Could a `getBy*` role/label/text locator replace this CSS/XPath locator?
- If this is an id/CSS fallback, is there a comment explaining why (no `data-testid` available)?
- Is a `data-testid` worth recommending to the app team for this element?
