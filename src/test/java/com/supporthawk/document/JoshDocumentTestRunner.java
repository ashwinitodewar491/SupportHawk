package com.supporthawk.document;

import com.microsoft.playwright.Page;
import com.supporthawk.config.TenantRoutes;
import com.supporthawk.data.DocumentQuerySet;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.data.QueryTagFilter;
import com.supporthawk.pages.LoginPage;
import com.supporthawk.pages.QueryPage;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared orchestration for Josh document-query tests driven by document_queries.json.
 * Admin post-login: upload/prepare, run queries, delete.
 * Josh pre-login: navigate to Josh /query and run queries against existing documents.
 * Both suites honor {@code -DtestGroups} via {@link QueryTagFilter}.
 */
public final class JoshDocumentTestRunner {

    private static final String DOCUMENT_QUERIES_FILE = "document_queries.json";

    private JoshDocumentTestRunner() {
    }

    /**
     * Runs the full admin document lifecycle + query loop for the given test identity.
     *
     * @param page                 Playwright page (already admin-logged-in)
     * @param testClass            simple test class name for reporting metadata
     * @param testMethod           test method name for reporting metadata
     * @param failureSummaryPrefix exact prefix used in the final Assert.fail message
     */
    public static void runDocumentQuerySuite(
            Page page,
            String testClass,
            String testMethod,
            String failureSummaryPrefix
    ) throws Exception {
        LoginPage loginPage = new LoginPage(page);
        List<DocumentQuerySet> documents = loadFilteredDocuments();

        List<String> failures = new ArrayList<>();

        for (DocumentQuerySet documentSet : documents) {
            processDocument(page, loginPage, documentSet, failures, testClass, testMethod);
        }

        failIfAny(failures, failureSummaryPrefix);
    }

    /**
     * Josh pre-login: open Josh {@code /query} with no login, then run document_queries.json
     * questions against documents already present for the tenant (no upload/delete).
     */
    public static void runPreLoginDocumentQuerySuite(
            Page page,
            String testClass,
            String testMethod,
            String failureSummaryPrefix
    ) {
        new QueryPage(page).navigate(TenantRoutes.Tenant.JOSH);

        List<DocumentQuerySet> documents = loadFilteredDocuments();

        List<String> failures = new ArrayList<>();

        for (DocumentQuerySet documentSet : documents) {
            List<QueryModel> queries = documentSet.getQueries();
            Assert.assertNotNull(queries, "queries missing for document: " + documentSet.getDocument());
            Assert.assertFalse(queries.isEmpty(), "queries empty for document: " + documentSet.getDocument());

            String documentTitle = documentSet.getDocument();
            Assert.assertNotNull(documentTitle, "document title missing");
            Assert.assertFalse(documentTitle.isBlank(), "document title blank");

            DocumentQueryRunner.executeDocumentQueriesOnCurrentPage(
                    page,
                    documentTitle,
                    queries,
                    failures,
                    testClass,
                    testMethod,
                    "Pre-login"
            );
        }

        failIfAny(failures, failureSummaryPrefix);
    }

    /**
     * Loads document_queries.json and applies the same {@code -DtestGroups} filter
     * used by Fintech DataProviders (query-level tags; empty group → all).
     */
    private static List<DocumentQuerySet> loadFilteredDocuments() {
        List<DocumentQuerySet> documents = QueryTagFilter.filterDocumentSets(
                QueryData.getDocumentQueries(DOCUMENT_QUERIES_FILE)
        );
        String group = QueryTagFilter.requestedTestGroups();
        String message = QueryTagFilter.isFilterActive()
                ? "No documents/queries matched -DtestGroups=" + group.trim()
                + " in " + DOCUMENT_QUERIES_FILE
                : "No documents found in " + DOCUMENT_QUERIES_FILE;
        Assert.assertFalse(documents.isEmpty(), message);
        return documents;
    }

    private static void processDocument(
            Page page,
            LoginPage loginPage,
            DocumentQuerySet documentSet,
            List<String> failures,
            String testClass,
            String testMethod
    ) throws Exception {
        List<QueryModel> queries = documentSet.getQueries();
        Assert.assertNotNull(queries, "queries missing for document: " + documentSet.getDocument());
        Assert.assertFalse(queries.isEmpty(), "queries empty for document: " + documentSet.getDocument());

        String documentTitle = DocumentLifecycleManager.uploadAndPrepareDocument(
                page, loginPage, documentSet
        );

        try {
            DocumentQueryRunner.executeDocumentQueries(
                    page,
                    loginPage,
                    documentTitle,
                    queries,
                    failures,
                    testClass,
                    testMethod
            );
        } finally {
            // Always delete this document before the next JSON entry is processed.
            DocumentLifecycleManager.deleteDocument(loginPage, documentTitle);
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
