package com.supporthawk.fintech.prelogin;

import com.supporthawk.config.TenantRoutes;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.report.QueryReportRecorder;
import com.supporthawk.report.QueryResult;
import com.supporthawk.utils.KeywordValidator;
import com.supporthawk.utils.ScreenshotUtil;
import org.testng.Assert;

import java.util.List;

/**
 * Shared crawl-based Fintech pre-login query execution and validation.
 * Text and voice keep their existing ask mechanisms; keyword rules,
 * reference validation, and reporting hooks match the previous inline test logic.
 */
public final class FintechPreLoginQueryRunner {

    private FintechPreLoginQueryRunner() {
    }

    /**
     * Fintech pre-login text flow: navigate, ask via text, validate keywords and references.
     */
    public static void executeTextQuery(
            QueryPage queryPage,
            QueryModel queryModel,
            String testClass,
            String testMethod
    ) {
        QueryResult qr = QueryReportRecorder.begin(
                "Pre-login",
                testClass,
                testMethod,
                queryModel
        );

        queryPage.navigate(TenantRoutes.Tenant.FINTECH);
        String response = queryPage.askQuestion(queryModel.getQuery());
        qr.setActualResponse(response);

        validateKeywordsAndReferences(
                queryPage,
                queryModel,
                response,
                qr,
                "Not enough keywords matched in BOT RESPONSE. Needed at least "
        );
    }

    /**
     * Fintech pre-login voice flow: navigate, ask via voice, validate keywords and references.
     */
    public static void executeVoiceQuery(
            QueryPage queryPage,
            QueryModel queryModel,
            String testClass,
            String testMethod
    ) {
        QueryResult qr = QueryReportRecorder.begin(
                "Voice",
                testClass,
                testMethod,
                queryModel
        );

        queryPage.navigate(TenantRoutes.Tenant.FINTECH);
        String response = queryPage.askVoiceQuestion(queryModel.getQuery());
        qr.setActualResponse(response);

        validateKeywordsAndReferences(
                queryPage,
                queryModel,
                response,
                qr,
                "Not enough keywords matched in BOT RESPONSE for voice query. Needed at least "
        );
    }

    private static void validateKeywordsAndReferences(
            QueryPage queryPage,
            QueryModel queryModel,
            String response,
            QueryResult qr,
            String keywordFailurePrefix
    ) {
        List<String> expectedKeywords = queryModel.getExpected();

        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        int totalMatched = matchedKeywords.size();
        boolean keywordValidationPassed = totalMatched >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("BOT RESPONSE KEYWORD VALIDATION");
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Required matches: " + requiredMatches);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Matched count: " + totalMatched);
        System.out.println("Keyword validation: " + (keywordValidationPassed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        qr.setExpectedResponse(String.valueOf(expectedKeywords));
        qr.setResponseValidationStatus(
                keywordValidationPassed
                        ? QueryResult.Status.PASSED.name()
                        : QueryResult.Status.FAILED.name()
        );

        String keywordFailureMessage = keywordFailurePrefix
                + requiredMatches
                + " but found " + totalMatched
                + ". Expected: " + expectedKeywords
                + ". Matched: " + matchedKeywords;

        if (!keywordValidationPassed) {
            qr.setErrorMessage(keywordFailureMessage);
            ScreenshotUtil.captureForLabel(queryPage.getPage(), queryModel.getQuery());
        }

        Assert.assertTrue(keywordValidationPassed, keywordFailureMessage);

        queryPage.provideRandomFeedback("This is automated test feedback for thumbs down.");

        queryPage.getPage().waitForTimeout(10000);
        try {
            queryPage.validateReferenceLinks(expectedKeywords);
            qr.setDocumentValidationStatus(QueryResult.Status.PASSED.name());
        } catch (AssertionError e) {
            qr.setDocumentValidationStatus(QueryResult.Status.FAILED.name());
            if (qr.getErrorMessage() == null || qr.getErrorMessage().isBlank()) {
                qr.setErrorMessage(e.getMessage());
            } else {
                qr.setErrorMessage(qr.getErrorMessage() + "\n" + e.getMessage());
            }
            ScreenshotUtil.captureForLabel(queryPage.getPage(), queryModel.getQuery());
            throw e;
        }
    }
}
