package com.supporthawk.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.supporthawk.config.AppConfig;
import com.supporthawk.config.ConfigReader;
import com.supporthawk.utils.EdgeTTSUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Page object for the SupportHawk Query screen.
 * Holds locators and actions for asking a question and reading the AI reply.
 */
public class QueryPage {

    private final Page page;

    // Locators
    private final String queryBox = "textarea.flex-1";
    private final String sendButton = "button[aria-label='Send message']";
    private final String processingIndicator = "div.animate-pulse";
    private final String holdMicrophoneButton = "button[title='Record voice input']";
    private final String stopMicrophoneButton = "button[title='Stop recording']";
    private final String userMessage = "div.px-5.py-3.rounded-3xl.bg-muted";
    private final String referencesLabel = "text=References:";
    private final String referenceLinks = "a[target='_blank'][href]";
    private final String thumbsUpButton = "button[title='Thumbs up']";
    private final String thumbsDownButton = "button[title='Thumbs down']";
    private final String feedbackTextArea = "textarea[placeholder='Share your thoughts...']";
    private final String submitFeedbackButton = "button:text-is('Submit')";
    private final String cancelFeedbackButton = "button:text-is('Cancel')";
    private final String feedbackSubmittedMessage =
        "div:text-is('Thank you for your feedback!')";
    /**
     * Parent container for each full AI reply.
     *
     * WHY this container (and not only paragraph / p tags):
     * An AI response can include paragraphs, tables, bullet lists, hyperlinks,
     * and references. If we located only p tags, we would miss tables and
     * other nested content. Selecting the parent div lets Playwright's
     * innerText() automatically collect ALL visible text inside the response,
     * including tables and any future response formats, without extra methods.
     */
    private final String responseContainer = "div.py-2.text-foreground.break-words.leading-relaxed.w-full";

    public QueryPage(Page page) {
        this.page = page;
    }

    /** Returns the underlying Playwright page (used by shared test runners). */
    public Page getPage() {
        return page;
    }

    /** Opens the Query page. */
    public void navigate() {
        page.navigate(AppConfig.BASE_URL + "/query");
    }

    /** Types the question into the query box. */
    public void enterQuery(String query) {
        page.locator(queryBox).fill(query);
    }

    /** Clicks the Send button. */
    public void clickSend() {
        page.locator(sendButton).click();
    }

    /** Waits until the "processing" indicator disappears. */
    public void waitForResponse() {
        page.locator(processingIndicator).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
        );
        // Give the UI time to finish rendering
        page.waitForTimeout(2000);
    }

    /**
     * Returns the COMPLETE latest AI response as plain text.
     * Uses responseContainer.last() so we get the newest reply, then
     * innerText() so nested paragraphs, tables, lists, links, and references
     * are all included in one string.
     */
    public String getLatestResponse() {
        waitForResponse();

        Locator responses = page.locator(responseContainer);
        responses.last().waitFor();

        return responses.last().innerText();
    }

    /** Returns column headers from the latest response table, if present. */
    public List<String> getLatestResponseTableHeaders() {
        Locator table = getLatestResponseTable();
        if (table == null) {
            return List.of();
        }

        Locator headerCells = table.locator("thead tr th");
        if (headerCells.count() == 0) {
            headerCells = table.locator("tr th");
        }
        List<String> headers = new ArrayList<>();
        int count = headerCells.count();
        for (int i = 0; i < count; i++) {
            String text = headerCells.nth(i).innerText();
            headers.add(text == null ? "" : text.trim());
        }
        return headers;
    }

    /** Returns row cell text from the latest response table, if present. */
    public List<List<String>> getLatestResponseTableRows() {
        Locator table = getLatestResponseTable();
        if (table == null) {
            return List.of();
        }

        Locator rows = table.locator("tbody tr");
        if (rows.count() == 0) {
            rows = table.locator("tr").filter(new Locator.FilterOptions().setHas(page.locator("td")));
        }
        int rowCount = rows.count();
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            Locator cells = rows.nth(i).locator("td");
            int cellCount = cells.count();
            List<String> row = new ArrayList<>();
            for (int j = 0; j < cellCount; j++) {
                String text = cells.nth(j).innerText();
                row.add(text == null ? "" : text.trim());
            }
            if (!row.isEmpty()) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Finds the latest visible transaction table by confirmed structure:
     * table &gt; thead with Date and Description headers, plus tbody rows.
     * Does not assume the table is a direct descendant or sibling of
     * {@code responseContainer}.
     */
    private Locator getLatestResponseTable() {
        waitForResponse();
        Locator tables = page.locator("table")
                .filter(new Locator.FilterOptions().setHas(
                        page.locator("thead th").filter(new Locator.FilterOptions().setHasText("Date"))))
                .filter(new Locator.FilterOptions().setHas(
                        page.locator("thead th").filter(new Locator.FilterOptions().setHasText("Description"))))
                .filter(new Locator.FilterOptions().setHas(page.locator("tbody tr")));

        try {
            tables.last().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        } catch (Exception e) {
            return null;
        }
        if (tables.count() == 0) {
            return null;
        }
        return tables.last();
    }

    public void giveThumbsUp() {
        page.locator(thumbsUpButton).last().click();
    }

    public void giveThumbsDown(String feedback) {
        page.locator(thumbsDownButton).last().click();
        page.locator(feedbackTextArea).fill(feedback);
        page.locator(submitFeedbackButton).click();
    }

    /**
     * Waits until the thumbs-up and thumbs-down feedback controls are visible.
     * AI response text can appear (and keyword validation can run) before these
     * controls are rendered alongside References.
     */
    public void waitForFeedbackControls() {
        page.locator(thumbsUpButton).last().waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
        );
        page.locator(thumbsDownButton).last().waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
        );
    }

    /**
     * Randomly provides either a thumbs up or a thumbs down (with feedback).
     * This ensures the feedback flow is tested with a mix of both actions
     * across different queries.
     *
     * @param feedback The text to provide if a thumbs down is randomly selected.
     */
    public void provideRandomFeedback(String feedback) {
        waitForFeedbackControls();
        if (Math.random() < 0.5) {
            giveThumbsUp();
        } else {
            giveThumbsDown(feedback);
        }
    }

    public boolean isFeedbackSubmitted() {
        return page.locator(feedbackSubmittedMessage).isVisible();
    }

    /**
     * Validates reference links in the latest AI response by navigating to each href
     * and confirming the page loads with body content. Checks every reference link even
     * when one fails. Skips without failing when no "References:" section or links
     * are present.
     *
     * @param expectedKeywords kept for API compatibility; not used in reference pass/fail logic
     */
    public void validateReferenceLinks(List<String> expectedKeywords) {
        Locator latestResponse = page.locator(responseContainer).last();
        Locator messageContainer = latestResponse.locator("xpath=..");
        int referencesCount = messageContainer.locator(referencesLabel).count();

        if (referencesCount == 0) {
            return;
        }

        Locator links = messageContainer.locator(referenceLinks);
        int linkCount = links.count();
        if (linkCount == 0) {
            return;
        }

        List<String> hrefs = new ArrayList<>();
        for (int i = 0; i < linkCount; i++) {
            String href = links.nth(i).getAttribute("href");
            if (href != null && !href.isBlank()) {
                hrefs.add(href);
            }
        }

        if (hrefs.isEmpty()) {
            return;
        }

        String queryPageUrl = page.url();
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < hrefs.size(); i++) {
            String href = hrefs.get(i);
            String failureReason = null;

            try {
                page.navigate(href);
                page.waitForLoadState();

                String pageContent = page.locator("body").innerText();
                if (pageContent == null || pageContent.trim().isEmpty()) {
                    failureReason = "reference page has no content";
                }
            } catch (Exception e) {
                failureReason = "navigation or page error: " + e.getMessage();
            }

            boolean passed = failureReason == null;

            System.out.println("--------------------------------------------------");
            System.out.println("Reference URL: " + href);
            System.out.println("Reference page loaded: " + (passed ? "PASSED" : "FAILED"));
            System.out.println("Reference validation: " + (passed ? "PASSED" : "FAILED"));
            if (!passed) {
                System.out.println("Failure reason: " + failureReason);
            }
            System.out.println("--------------------------------------------------");

            if (!passed) {
                failures.add(href + " — " + failureReason);
            }

            restoreQueryPage(queryPageUrl);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "Reference validation failed for " + failures.size() + " of " + hrefs.size() + " link(s):\n"
                            + String.join("\n", failures)
            );
        }
    }

    /**
     * Restores the SupportHawk query page after opening a reference link.
     * Uses direct navigation when the current URL differs, so a failed reference
     * navigation never triggers an accidental goBack() away from the query page.
     */
    private void restoreQueryPage(String queryPageUrl) {
        try {
            if (!page.url().equals(queryPageUrl)) {
                page.navigate(queryPageUrl);
                page.waitForLoadState();
            }
        } catch (Exception e) {
            System.out.println("Warning: could not restore query page: " + e.getMessage());
        }
    }

    /**
     * Clicks the reference/source link for the latest AI response, verifies the
     * referenced PDF opens, and asserts the opened document title matches
     * {@code expectedDocumentTitle}, then returns to the Query conversation page.
     *
     * The MinIO/PDF tab does not expose the SupportHawk document title as DOM text
     * or {@code page.title()} (Chrome PDF embed has an empty body). The title is
     * taken from the reference link that opened the tab — that is the displayed
     * document identity for the opened reference.
     */
    public void openAndVerifyLatestDocumentReference(String expectedDocumentTitle) {
        Locator messageContainer = page.locator(responseContainer).last().locator("xpath=..");

        // References can render after the answer text; wait for the section explicitly.
        messageContainer.locator(referencesLabel).waitFor();

        Locator links = messageContainer.locator(referenceLinks);
        if (links.count() == 0) {
            throw new AssertionError(
                    "References section has no links for the latest bot response.\n"
                            + "Expected document title: " + expectedDocumentTitle + "\n"
                            + "Actual document title: <no reference link>"
            );
        }

        // Prefer a link whose visible text matches the uploaded document title.
        Locator matchingByTitle = links.filter(
                new Locator.FilterOptions().setHasText(expectedDocumentTitle)
        );
        Locator linkToClick = matchingByTitle.count() > 0 ? matchingByTitle.first() : links.first();

        String queryPageUrl = page.url();
        Page documentPage;
        boolean openedAsPopup;

        try {
            documentPage = page.waitForPopup(
                    new Page.WaitForPopupOptions().setTimeout(15000),
                    linkToClick::click
            );
            openedAsPopup = true;
        } catch (Exception popupTimeout) {
            linkToClick.click();
            page.waitForLoadState();
            documentPage = page;
            openedAsPopup = false;
        }

        documentPage.waitForLoadState();

        boolean documentOpened = isDocumentViewerOpen(documentPage);
        String actualDocumentTitle = resolveOpenedDocumentTitle(documentPage, linkToClick);

        System.out.println("--------------------------------------------------");
        System.out.println("Reference URL: " + documentPage.url());
        System.out.println("Document viewer open: " + documentOpened);
        System.out.println("Expected document title: " + expectedDocumentTitle);
        System.out.println("Actual document title: " + actualDocumentTitle);
        System.out.println("--------------------------------------------------");

        try {
            if (!documentOpened) {
                throw new AssertionError(
                        "Referenced document/PDF did not open.\n"
                                + "Expected document title: " + expectedDocumentTitle + "\n"
                                + "Opened URL: " + documentPage.url()
                );
            }

            boolean titleMatches = actualDocumentTitle != null
                    && normalizeText(actualDocumentTitle).equals(normalizeText(expectedDocumentTitle));

            if (!titleMatches) {
                throw new AssertionError(
                        "Expected document title: " + expectedDocumentTitle + "\n"
                                + "Actual document title: "
                                + (actualDocumentTitle == null ? "<not found>" : actualDocumentTitle)
                );
            }
        } finally {
            if (openedAsPopup) {
                documentPage.close();
            } else {
                restoreQueryPage(queryPageUrl);
            }
        }

        // Ensure the Query input is ready for the next question.
        page.locator(queryBox).waitFor();
    }

    /**
     * Resolves the title for the document opened in {@code documentPage}.
     * Prefers {@code page.title()} when the opened tab sets one; otherwise uses the
     * reference-link title (the PDF embed exposes no readable title in the DOM).
     */
    private String resolveOpenedDocumentTitle(Page documentPage, Locator referenceLink) {
        String tabTitle = documentPage.title();
        if (tabTitle != null && !tabTitle.isBlank()) {
            return tabTitle.trim();
        }
        return extractReferenceLinkTitle(referenceLink);
    }

    private String extractReferenceLinkTitle(Locator link) {
        // Reference links render as: <a>...<span>icon</span><span>Document Title</span></a>
        Locator titleSpan = link.locator("span").last();
        if (titleSpan.count() > 0) {
            String text = titleSpan.innerText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        String text = link.innerText();
        return text == null ? null : text.replace("?", "").trim();
    }

    private boolean isDocumentViewerOpen(Page documentPage) {
        String url = documentPage.url() == null ? "" : documentPage.url().toLowerCase();
        if (url.contains(".pdf") || url.contains("/documents/") || url.contains("blob:")) {
            return true;
        }
        if (documentPage.locator("embed, object, iframe, canvas").count() > 0) {
            return true;
        }
        String body = documentPage.locator("body").innerText();
        return body != null && !body.trim().isEmpty();
    }

    /**
     * Convenience helper: enter the question, send it, and return the reply.
     */
    public String askQuestion(String query) {
        enterQuery(query);
        clickSend();
        return getLatestResponse();
    }

    /**
     * Voice flow: click microphone, hold while fake WAV mic audio is consumed,
     * click stop, validate the transcribed user chat message, wait for AI response,
     * then return the latest response.
     *
     * @param query text query that will be converted to speech
     * @return latest AI response text
     */
    public String askVoiceQuestion(String query) {
        String wavPath = System.getProperty("current.voice.wav.path");
        if (wavPath == null || wavPath.isBlank()) {
            throw new RuntimeException(
                    "Voice WAV path not configured for this test run. " +
                            "Ensure voice tests run with QueryModel data so BasePage can prepare fake audio capture."
            );
        }

        Path wavFile = Path.of(wavPath);
        long wavDurationMs = EdgeTTSUtil.getWavDurationMs(wavFile);
        long holdBufferMs = Long.parseLong(ConfigReader.get("voice.hold.buffer.ms"));
        long totalHoldMs = wavDurationMs + holdBufferMs;

        int previousUserMessageCount = page.locator(userMessage).count();
        int previousResponseCount = page.locator(responseContainer).count();

        page.locator(holdMicrophoneButton).click();
        page.waitForTimeout(totalHoldMs);
        page.locator(stopMicrophoneButton).click();

        waitForNewUserMessage(previousUserMessageCount);

        String transcribed = page.locator(userMessage).last().innerText().trim();
        String expectedNormalized = normalizeText(query);
        String actualNormalized = normalizeText(transcribed);

        if (!expectedNormalized.equals(actualNormalized)) {
            throw new AssertionError(
                    "Voice transcription mismatch.\n"
                            + "Expected: " + query + "\n"
                            + "Actual: " + transcribed
            );
        }

        waitForNewResponse(previousResponseCount);
        return getLatestResponse();
    }

    private void waitForNewUserMessage(int previousCount) {
        long timeoutMs = Long.parseLong(ConfigReader.get("voice.transcription.timeout.ms"));
        try {
            page.waitForFunction(
                    "([selector, oldCount]) => document.querySelectorAll(selector).length > oldCount",
                    Arrays.asList(userMessage, previousCount),
                    new Page.WaitForFunctionOptions().setTimeout((double) timeoutMs)
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "User chat message did not appear after voice query within " + timeoutMs + " ms.",
                    e
            );
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private void waitForNewResponse(int previousCount) {
        long timeoutMs = Long.parseLong(ConfigReader.get("voice.response.timeout.ms"));
        try {
            page.waitForFunction(
                    "([selector, oldCount]) => document.querySelectorAll(selector).length > oldCount",
                    Arrays.asList(responseContainer, previousCount),
                    new Page.WaitForFunctionOptions().setTimeout((double) timeoutMs)
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "AI response did not appear after voice query within " + timeoutMs + " ms.",
                    e
            );
        }
    }
}
