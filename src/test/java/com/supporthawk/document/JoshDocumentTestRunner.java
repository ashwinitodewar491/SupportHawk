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
 *
 * <p>Admin upload+query lives here (same path as before). Document delete for the
 * shared Admin + pre-login flow is owned by {@link JoshDocumentFlowBase} {@code @AfterGroups}.
 */
public final class JoshDocumentTestRunner {

    private static final String DOCUMENT_QUERIES_FILE = "document_queries.json";

    private JoshDocumentTestRunner() {
    }

    /**
     * Admin path: for each filtered document, upload/prepare and run queries.
     * Does <strong>not</strong> delete — suite cleanup deletes after pre-login finishes.
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
     * Deletes every filtered document. Used by Josh flow {@code @AfterGroups} cleanup.
     */
    public static void deleteAllFilteredDocuments(Page page) {
        LoginPage loginPage = new LoginPage(page);
        for (DocumentQuerySet documentSet : loadFilteredDocuments()) {
            String documentTitle = documentSet.getDocument();
            if (documentTitle == null || documentTitle.isBlank()) {
                continue;
            }
            DocumentLifecycleManager.deleteDocument(loginPage, documentTitle);
        }
    }

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

        // Existing working Admin upload path (preview + edit included). Keep document for pre-login.
        String documentTitle = DocumentLifecycleManager.uploadAndPrepareDocument(page, loginPage, documentSet);

        DocumentQueryRunner.executeDocumentQueries(
                page,
                loginPage,
                documentTitle,
                queries,
                failures,
                testClass,
                testMethod
        );
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
