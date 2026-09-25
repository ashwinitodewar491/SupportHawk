package com.supporthawk.context;

import com.microsoft.playwright.Page;
import com.supporthawk.config.TenantRoutes;
import com.supporthawk.data.ContextConversation;
import com.supporthawk.data.ContextQuestion;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryTagFilter;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.report.QueryResult;
import com.supporthawk.report.QueryResultCollector;
import com.supporthawk.utils.KeywordValidator;
import com.supporthawk.utils.ScreenshotUtil;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs multi-turn context-retention conversations on Fintech /query in one chat session.
 * Navigates once, then asks each question sequentially on the same Page — no reload,
 * no new tab, no browser relaunch between turns.
 */
public final class ContextRetentionRunner {

    private static final String CONTEXT_QUERIES_FILE = "context_retention_queries.json";

    private ContextRetentionRunner() {
    }

    /**
     * Fintech pre-login: open Fintech {@code /query} once, then run each matching conversation
     * as a continuous soft-failure flow. No admin or customer login.
     */
    public static void runFintechPreLoginContextSuite(
            Page page,
            String testClass,
            String testMethod,
            String failureSummaryPrefix
    ) {
        List<ContextConversation> conversations = loadFilteredConversations();
        List<String> failures = new ArrayList<>();

        // Single Page / tab for the entire suite invocation (BasePage already launched once).
        QueryPage queryPage = new QueryPage(page);
        int pageIdentity = System.identityHashCode(page);

        System.out.println("[Context Flow] Starting Home Loan context test");
        System.out.println("[Context Flow] Using Page: " + pageIdentity);
        System.out.println("[Context Flow] Navigating once to Fintech pre-login /query");
        queryPage.navigate(TenantRoutes.Tenant.FINTECH);
        String sessionUrl = page.url();
        System.out.println("[Context Flow] Session URL after navigate: " + sessionUrl);

        for (ContextConversation conversation : conversations) {
            executeConversation(
                    page,
                    queryPage,
                    conversation,
                    failures,
                    testClass,
                    testMethod,
                    pageIdentity,
                    sessionUrl
            );
        }

        System.out.println("[Context Flow] Completed");
        System.out.println("[Context Flow] Final Page: " + System.identityHashCode(page)
                + " (expected same as start: " + pageIdentity + ")");
        failIfAny(failures, failureSummaryPrefix);
    }

    private static List<ContextConversation> loadFilteredConversations() {
        List<ContextConversation> conversations = QueryTagFilter.filterContextConversations(
                QueryData.getContextConversations(CONTEXT_QUERIES_FILE)
        );
        String group = QueryTagFilter.requestedTestGroups();
        String message = QueryTagFilter.isFilterActive()
                ? "No context conversations matched -DtestGroups=" + group.trim()
                + " in " + CONTEXT_QUERIES_FILE
                : "No context conversations found in " + CONTEXT_QUERIES_FILE;
        Assert.assertFalse(conversations.isEmpty(), message);
        return conversations;
    }

    /**
     * Asks every question in {@code conversation} on the same QueryPage instance
     * (same browser page / chat). Does not navigate between turns.
     */
    private static void executeConversation(
            Page page,
            QueryPage queryPage,
            ContextConversation conversation,
            List<String> failures,
            String testClass,
            String testMethod,
            int pageIdentity,
            String sessionUrl
    ) {
        List<ContextQuestion> questions = conversation.getQuestions();
        Assert.assertNotNull(questions, "questions missing for conversation: " + conversation.getName());
        Assert.assertFalse(
                questions.isEmpty(),
                "questions empty for conversation: " + conversation.getName()
        );

        int total = questions.size();
        System.out.println("==================================================");
        System.out.println("CONTEXT CONVERSATION: " + conversation.getName());
        System.out.println("Intent: " + conversation.getIntent());
        System.out.println("Turns: " + total);
        System.out.println("==================================================");

        for (int i = 0; i < total; i++) {
            ContextQuestion question = questions.get(i);
            assertSameSession(page, pageIdentity, sessionUrl, i + 1, total);
            System.out.println("[Context Flow] Q" + (i + 1) + "/" + total
                    + " Page: " + System.identityHashCode(page));
            executeTurn(page, queryPage, conversation, question, failures, testClass, testMethod);
        }
    }

    private static void assertSameSession(
            Page page,
            int expectedPageIdentity,
            String sessionUrl,
            int turn,
            int total
    ) {
        int actual = System.identityHashCode(page);
        Assert.assertEquals(
                actual,
                expectedPageIdentity,
                "[Context Flow] Page object changed between turns at Q" + turn + "/" + total
                        + " (expected identity " + expectedPageIdentity + ", got " + actual + ")"
        );
        Assert.assertFalse(
                page.isClosed(),
                "[Context Flow] Page was closed before Q" + turn + "/" + total
        );
        // Do not re-navigate; only verify we never left the query chat for a new URL origin path.
        String current = page.url();
        Assert.assertTrue(
                current != null && current.contains("/query"),
                "[Context Flow] Left /query URL before Q" + turn + "/" + total
                        + ". Started at: " + sessionUrl + " Current: " + current
        );
    }

    private static void executeTurn(
            Page page,
            QueryPage queryPage,
            ContextConversation conversation,
            ContextQuestion question,
            List<String> failures,
            String testClass,
            String testMethod
    ) {
        long startedMs = System.currentTimeMillis();
        QueryResult qr = new QueryResult();
        qr.setQuery(question.getQuery());
        qr.setFlow("Pre-login");
        qr.setTestClass(testClass);
        qr.setTestMethod(testMethod);
        qr.setLanguage(conversation.getLanguage());
        qr.setIntent(conversation.getIntent());
        qr.setStartTime(QueryResultCollector.formatEpoch(startedMs));

        String questionId = question.getId() != null ? question.getId() : "-";

        try {
            // Same chat: no navigate, no new Page, no conversation clear.
            String response = queryPage.askQuestionInSameChat(question.getQuery());
            recordKeywordValidation(page, qr, conversation, question, questionId, response, failures);
        } catch (AssertionError | RuntimeException queryFailure) {
            if (qr.getResponseValidationStatus() == null) {
                qr.setResponseValidationStatus(QueryResult.Status.FAILED.name());
            }
            if (qr.getErrorMessage() == null || qr.getErrorMessage().isBlank()) {
                qr.setErrorMessage(queryFailure.getMessage());
            }
            String detail = "Conversation: " + conversation.getName() + "\n"
                    + "   Question ID: " + questionId + "\n"
                    + "   Query: " + question.getQuery() + "\n"
                    + "   Context dependent: " + question.isContextDependency() + "\n"
                    + "   Error: " + queryFailure.getMessage();
            failures.add(detail);
            if (qr.getScreenshotPath() == null || qr.getScreenshotPath().isBlank()) {
                qr.setScreenshotPath(ScreenshotUtil.captureForLabel(page, questionId + "_" + question.getQuery()));
            }
            // Soft-fail: stay on the same Page and continue with the next turn.
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

    private static void recordKeywordValidation(
            Page page,
            QueryResult qr,
            ContextConversation conversation,
            ContextQuestion question,
            String questionId,
            String response,
            List<String> failures
    ) {
        List<String> expectedKeywords = question.getExpectedKeywords();
        if (expectedKeywords == null) {
            expectedKeywords = List.of();
        }
        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        boolean passed = matchedKeywords.size() >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("Conversation: " + conversation.getName());
        System.out.println("Question ID: " + questionId);
        System.out.println("Query: " + question.getQuery());
        System.out.println("Context dependent: " + question.isContextDependency());
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Required matches: " + requiredMatches);
        System.out.println("Actual response: " + response);
        System.out.println("Keyword validation: " + (passed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        qr.setExpectedResponse(String.valueOf(expectedKeywords));
        qr.setActualResponse(response);
        qr.setResponseValidationStatus(
                passed ? QueryResult.Status.PASSED.name() : QueryResult.Status.FAILED.name()
        );

        if (!passed) {
            String error = "Conversation: " + conversation.getName() + "\n"
                    + "   Question ID: " + questionId + "\n"
                    + "   Query: " + question.getQuery() + "\n"
                    + "   Context dependent: " + question.isContextDependency() + "\n"
                    + "   Expected keywords: " + expectedKeywords + "\n"
                    + "   Matched keywords: " + matchedKeywords + "\n"
                    + "   Required matches: " + requiredMatches + "\n"
                    + "   Actual response: " + response;
            failures.add(error);
            qr.setErrorMessage(error);
            qr.setScreenshotPath(ScreenshotUtil.captureForLabel(page, questionId + "_" + question.getQuery()));
        }
    }

    private static void failIfAny(List<String> failures, String failureSummaryPrefix) {
        if (!failures.isEmpty()) {
            StringBuilder summary = new StringBuilder(failureSummaryPrefix);
            for (int i = 0; i < failures.size(); i++) {
                summary.append("\n").append(i + 1).append(". ").append(failures.get(i));
                if (i < failures.size() - 1) {
                    summary.append("\n");
                }
            }
            Assert.fail(summary.toString());
        }
    }
}
