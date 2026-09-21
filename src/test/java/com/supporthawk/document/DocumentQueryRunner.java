package com.supporthawk.document;

import com.microsoft.playwright.Page;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.LoginPage;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.report.QueryResult;
import com.supporthawk.report.QueryResultCollector;
import com.supporthawk.utils.KeywordValidator;
import com.supporthawk.utils.ScreenshotUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document-query execution and soft-failure validation for Admin document tests.
 * Preserves the previous AdminTest per-query behavior and reporting fields.
 */
public final class DocumentQueryRunner {

    private static final Pattern ACTUAL_DOCUMENT_TITLE = Pattern.compile(
            "Actual document title:\\s*(.+)", Pattern.CASE_INSENSITIVE
    );

    private DocumentQueryRunner() {
    }

    /**
     * Navigates to the Query page (logged-in UI) and runs each document query with soft continuation.
     * Keyword and document/reference failures are recorded into {@code failures}
     * without aborting the remaining queries.
     */
    public static void executeDocumentQueries(
            Page page,
            LoginPage loginPage,
            String documentTitle,
            List<QueryModel> queries,
            List<String> failures,
            String testClass,
            String testMethod
    ) {
        loginPage.goToQueryPage();
        executeDocumentQueriesOnCurrentPage(
                page, documentTitle, queries, failures, testClass, testMethod, "Admin"
        );
    }

    /**
     * Runs document queries on the already-open Query page (no login navigation).
     * Used by Josh pre-login against documents already available in the tenant.
     */
    public static void executeDocumentQueriesOnCurrentPage(
            Page page,
            String documentTitle,
            List<QueryModel> queries,
            List<String> failures,
            String testClass,
            String testMethod,
            String flow
    ) {
        QueryPage queryPage = new QueryPage(page);

        for (QueryModel queryModel : queries) {
            executeDocumentQuery(
                    page, queryPage, documentTitle, queryModel, failures, testClass, testMethod, flow
            );
        }
    }

    /**
     * Runs one document query with soft continuation: response and document/reference
     * failures are recorded and do not abort remaining queries.
     */
    private static void executeDocumentQuery(
            Page page,
            QueryPage queryPage,
            String documentTitle,
            QueryModel queryModel,
            List<String> failures,
            String testClass,
            String testMethod,
            String flow
    ) {
        long startedMs = System.currentTimeMillis();
        QueryResult qr = new QueryResult();
        qr.setQuery(queryModel.getQuery());
        qr.setFlow(flow);
        qr.setTestClass(testClass);
        qr.setTestMethod(testMethod);
        qr.setLanguage(null);
        qr.setIntent(null);
        qr.setStartTime(QueryResultCollector.formatEpoch(startedMs));

        try {
            String response = queryPage.askQuestion(queryModel.getQuery());
            recordResponseValidation(page, qr, documentTitle, queryModel, response, failures);

            try {
                queryPage.openAndVerifyLatestDocumentReference(documentTitle);
                qr.setExpectedDocument(documentTitle);
                qr.setActualDocument(documentTitle);
                qr.setDocumentValidationStatus(QueryResult.Status.PASSED.name());
            } catch (AssertionError docFailure) {
                recordDocumentValidationFailure(
                        page, qr, documentTitle, queryModel, docFailure, failures
                );
            }
        } catch (AssertionError | RuntimeException queryFailure) {
            if (qr.getResponseValidationStatus() == null) {
                qr.setResponseValidationStatus(QueryResult.Status.FAILED.name());
            }
            if (qr.getErrorMessage() == null || qr.getErrorMessage().isBlank()) {
                qr.setErrorMessage(queryFailure.getMessage());
            }
            String detail = "Document: " + documentTitle + "\n"
                    + "   Query: " + queryModel.getQuery() + "\n"
                    + "   Error: " + queryFailure.getMessage();
            failures.add(detail);
            if (qr.getScreenshotPath() == null || qr.getScreenshotPath().isBlank()) {
                qr.setScreenshotPath(ScreenshotUtil.captureForLabel(page, queryModel.getQuery()));
            }
        } finally {
            long endMs = System.currentTimeMillis();
            qr.setEndTime(QueryResultCollector.formatEpoch(endMs));
            qr.setDurationMs(Math.max(0L, endMs - startedMs));
            qr.refreshOverallStatus();
            if (qr.getOverallStatus() == null || qr.getOverallStatus().isBlank()) {
                qr.setOverallStatus(QueryResult.Status.FAILED.name());
            }
            QueryResultCollector.add(qr);
        }
    }

    /**
     * Same KeywordValidator rules as before; records expected/actual response and
     * PASS/FAIL before any assert can escape. Soft-records keyword misses.
     */
    private static void recordResponseValidation(
            Page page,
            QueryResult qr,
            String documentTitle,
            QueryModel queryModel,
            String response,
            List<String> failures
    ) {
        List<String> expectedKeywords = queryModel.getExpected();
        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        boolean passed = matchedKeywords.size() >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("Document: " + documentTitle);
        System.out.println("Query: " + queryModel.getQuery());
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Actual response: " + response);
        System.out.println("Keyword validation: " + (passed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        qr.setExpectedResponse(String.valueOf(expectedKeywords));
        qr.setActualResponse(response);
        qr.setResponseValidationStatus(
                passed ? QueryResult.Status.PASSED.name() : QueryResult.Status.FAILED.name()
        );

        if (!passed) {
            String error = "Document: " + documentTitle + "\n"
                    + "   Query: " + queryModel.getQuery() + "\n"
                    + "   Expected keywords: " + expectedKeywords + "\n"
                    + "   Actual response: " + response;
            failures.add(error);
            qr.setErrorMessage(error);
            qr.setScreenshotPath(ScreenshotUtil.captureForLabel(page, queryModel.getQuery()));
        }
    }

    private static void recordDocumentValidationFailure(
            Page page,
            QueryResult qr,
            String documentTitle,
            QueryModel queryModel,
            AssertionError docFailure,
            List<String> failures
    ) {
        String message = docFailure.getMessage() != null ? docFailure.getMessage() : "Document validation failed";
        qr.setExpectedDocument(documentTitle);
        qr.setActualDocument(extractActualDocumentTitle(message));
        qr.setDocumentValidationStatus(QueryResult.Status.FAILED.name());

        String error = "Document: " + documentTitle + "\n"
                + "   Query: " + queryModel.getQuery() + "\n"
                + "   Document/reference validation failed:\n"
                + "   " + message;
        failures.add(error);
        if (qr.getErrorMessage() == null || qr.getErrorMessage().isBlank()) {
            qr.setErrorMessage(error);
        } else {
            qr.setErrorMessage(qr.getErrorMessage() + "\n" + error);
        }
        if (qr.getScreenshotPath() == null || qr.getScreenshotPath().isBlank()) {
            qr.setScreenshotPath(ScreenshotUtil.captureForLabel(page, queryModel.getQuery()));
        }
    }

    private static String extractActualDocumentTitle(String message) {
        if (message == null || message.isBlank()) {
            return "<unknown>";
        }
        Matcher matcher = ACTUAL_DOCUMENT_TITLE.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return message;
    }
}
