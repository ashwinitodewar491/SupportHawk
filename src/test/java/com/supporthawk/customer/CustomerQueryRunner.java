package com.supporthawk.customer;

import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.utils.KeywordValidator;
import org.testng.Assert;

import java.util.List;
import java.util.Set;

/**
 * Shared execution and validation logic for customer query tests.
 * Routes by intent and performs intent-appropriate query execution before
 * shared keyword/feedback/reference validation.
 */
public final class CustomerQueryRunner {

    private static final Set<String> SIMPLE_INTENTS = Set.of(
            "balance_inquiry", "transaction_inquiry", "loan_inquiry"
    );

    private CustomerQueryRunner() {
    }

    public static void executeTextQuery(QueryPage queryPage, QueryModel queryModel) {
        logIntentStart(queryModel, "TEXT");
        String response = executeByIntent(queryPage, queryModel, false);
        validateByIntent(queryPage, queryModel, response);
    }

    public static void executeVoiceQuery(QueryPage queryPage, QueryModel queryModel) {
        logIntentStart(queryModel, "VOICE");
        String response = executeByIntent(queryPage, queryModel, true);
        validateByIntent(queryPage, queryModel, response);
    }

    private static String executeByIntent(QueryPage queryPage, QueryModel queryModel, boolean voice) {
        String intent = queryModel.getIntent();
        if (intent == null || intent.isBlank()) {
            String response = askInitial(queryPage, queryModel, voice);
            return DisambiguationResolver.resolve(queryPage, queryModel, response, voice);
        }
        if ("transfer_money".equals(intent)) {
            return TransferMoneyFlow.run(queryPage, queryModel, voice);
        }
        if (SIMPLE_INTENTS.contains(intent)) {
            String response = askInitial(queryPage, queryModel, voice);
            return DisambiguationResolver.resolve(queryPage, queryModel, response, voice);
        }
        System.out.println("WARNING: Unknown intent '" + intent
                + "' — falling through to default keyword validation for: "
                + queryModel.getQuery());
        String response = askInitial(queryPage, queryModel, voice);
        return DisambiguationResolver.resolve(queryPage, queryModel, response, voice);
    }

    private static String askInitial(QueryPage queryPage, QueryModel queryModel, boolean voice) {
        if (voice) {
            return queryPage.askVoiceQuestion(queryModel.getQuery());
        }
        return queryPage.askQuestion(queryModel.getQuery());
    }

    private static void validateByIntent(QueryPage queryPage, QueryModel queryModel, String response) {
        String intent = queryModel.getIntent();
        if ("transaction_inquiry".equals(intent)) {
            TransactionInquiryValidator.validate(queryPage, queryModel);
            runFeedbackAndReferenceValidation(queryPage, queryModel);
            return;
        }
        validateResponse(queryPage, queryModel, response);
    }

    private static void logIntentStart(QueryModel queryModel, String transport) {
        String intent = queryModel.getIntent();
        String language = queryModel.getLanguage();
        System.out.println("==================================================");
        System.out.println("INTENT: " + (intent != null ? intent : "unknown"));
        System.out.println("TRANSPORT: " + transport);
        System.out.println("LANGUAGE: " + (language != null ? language : "unknown"));
        System.out.println("QUERY: " + queryModel.getQuery());
        if (queryModel.getDisambiguationResponse() != null
                && !queryModel.getDisambiguationResponse().isBlank()) {
            System.out.println("DISAMBIGUATION: " + queryModel.getDisambiguationResponse());
        }
        System.out.println("==================================================");
    }

    private static void validateResponse(QueryPage queryPage, QueryModel queryModel, String response) {
        List<String> expectedKeywords = queryModel.getExpected();

        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        int totalMatched = matchedKeywords.size();
        boolean passed = totalMatched >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("BOT RESPONSE KEYWORD VALIDATION");
        System.out.println("Intent: " + queryModel.getIntent());
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Required matches: " + requiredMatches);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Matched count: " + totalMatched);
        System.out.println("Keyword validation: " + (passed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        Assert.assertTrue(
                passed,
                "Not enough keywords matched in BOT RESPONSE. Needed at least " + requiredMatches
                        + " but found " + totalMatched
                        + ". Expected: " + expectedKeywords
                        + ". Matched: " + matchedKeywords
        );

        runFeedbackAndReferenceValidation(queryPage, queryModel);
    }

    private static void runFeedbackAndReferenceValidation(QueryPage queryPage, QueryModel queryModel) {
        queryPage.provideRandomFeedback("This is automated test feedback for thumbs down.");
        queryPage.getPage().waitForTimeout(10000);
        queryPage.validateReferenceLinks(queryModel.getExpected());
    }
}
