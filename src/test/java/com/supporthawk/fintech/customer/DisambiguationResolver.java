package com.supporthawk.fintech.customer;

import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.utils.EdgeTTSUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Detects bot disambiguation prompts (account selection, loan selection) and
 * sends the configured follow-up response. Extracted from QueryPage so the
 * page object stays focused on UI interaction.
 */
final class DisambiguationResolver {

    private DisambiguationResolver() {
    }

    /**
     * If the bot response is a disambiguation prompt and the query has a configured
     * follow-up, sends that follow-up via text or voice and returns the new response.
     * Otherwise returns the original response unchanged.
     */
    static String resolve(QueryPage queryPage, QueryModel queryModel, String response, boolean voice) {
        String disambiguationResponse = queryModel.getDisambiguationResponse();
        if (disambiguationResponse == null || disambiguationResponse.isBlank()) {
            return response;
        }
        if (!isDisambiguationPrompt(response, queryModel.getIntent())) {
            return response;
        }

        System.out.println("Disambiguation prompt detected — sending follow-up: " + disambiguationResponse);

        return sendFollowUp(queryPage, disambiguationResponse, voice);
    }

    static String sendFollowUp(QueryPage queryPage, String followUpText, boolean voice) {
        if (voice) {
            prepareFollowUpVoiceAudio(followUpText);
            return queryPage.askVoiceQuestion(followUpText);
        }
        return queryPage.askQuestion(followUpText);
    }

    private static boolean isDisambiguationPrompt(String response, String intent) {
        if (response == null || response.isBlank() || intent == null || intent.isBlank()) {
            return false;
        }

        String lower = response.toLowerCase();
        if ("balance_inquiry".equals(intent) || "transaction_inquiry".equals(intent)) {
            return isAccountDisambiguation(lower);
        }
        if ("loan_inquiry".equals(intent)) {
            return isLoanDisambiguation(lower);
        }
        return false;
    }

    private static boolean isAccountDisambiguation(String lower) {
        if (lower.contains("loan")) {
            return false;
        }
        boolean asksToChoose = (lower.contains("choose") || lower.contains("which")) && lower.contains("account");
        boolean listsBothAccounts = lower.contains("current") && lower.contains("savings") && lower.contains("account");
        return asksToChoose || listsBothAccounts;
    }

    private static boolean isLoanDisambiguation(String lower) {
        return lower.contains("which loan");
    }

    private static void prepareFollowUpVoiceAudio(String followUpText) {
        String wavPath = System.getProperty("current.voice.wav.path");
        if (wavPath == null || wavPath.isBlank()) {
            throw new RuntimeException(
                    "Voice WAV path not configured for disambiguation follow-up. " +
                            "Ensure voice tests run with QueryModel data so BasePage can prepare fake audio capture."
            );
        }

        try {
            Path followUpWav = EdgeTTSUtil.generateWavFile(followUpText);
            Files.copy(followUpWav, Path.of(wavPath), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(followUpWav);
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare follow-up voice audio: " + e.getMessage(), e);
        }
    }
}
