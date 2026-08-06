package com.supporthawk.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.supporthawk.config.AppConfig;
import com.supporthawk.config.ConfigReader;
import com.supporthawk.utils.AudioPlayer;
import com.supporthawk.utils.EdgeTTSUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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

    String response = responses.last().textContent();

    // Print the response in the console
    System.out.println("===== AI RESPONSE =====");
    System.out.println(response);
    System.out.println("=======================");

    return response;
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
     * Voice flow: click microphone, play generated voice, stop recording,
     * wait for transcription and AI response, then return the latest response.
     * This keeps existing text flow intact and reusable.
     *
     * @param query text query that will be converted to speech
     * @return latest AI response text
     */
    public String askVoiceQuestion(String query) {
        Path audioFile = EdgeTTSUtil.generateSpeechFile(query);
        String previousInput = page.locator(queryBox).inputValue();
        int previousResponseCount = page.locator(responseContainer).count();

        try {
            page.locator(holdMicrophoneButton).click();

            // Playback is synchronous: this call returns only after audio ends.
            AudioPlayer.playMp3(audioFile);

            page.locator(stopMicrophoneButton).click();

            waitForTranscription(previousInput);
            waitForNewResponse(previousResponseCount);
            return getLatestResponse();
        } finally {
            try {
                Files.deleteIfExists(audioFile);
            } catch (Exception ignored) {
            }
        }
    }

    private void waitForTranscription(String previousInput) {
        long timeoutMs = Long.parseLong(ConfigReader.get("voice.transcription.timeout.ms"));
        try {
            page.waitForFunction(
                    "([selector, oldValue]) => {" +
                            "const el = document.querySelector(selector);" +
                            "if (!el) return false;" +
                            "const value = (el.value || '').trim();" +
                            "return value.length > 0 && value !== (oldValue || '');" +
                            "}",
                    Arrays.asList(queryBox, previousInput),
                    new Page.WaitForFunctionOptions().setTimeout((double) timeoutMs)
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Voice transcription did not appear in the query box within " + timeoutMs + " ms.",
                    e
            );
        }
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
