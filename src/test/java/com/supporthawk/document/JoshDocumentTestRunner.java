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
 * <p>Upload/delete for the shared Admin + pre-login flow is owned by
 * {@link JoshDocumentFlowBase}. This runner still supports a self-contained
 * upload→query→delete path via {@link #runDocumentQuerySuite}.
 */
public final class JoshDocumentTestRunner {

    private static final String DOCUMENT_QUERIES_FILE = "document_queries.json";

    private JoshDocumentTestRunner() {
    }

    /**
     * Self-contained admin path: for each filtered document, upload, query, then delete.
     * Prefer the shared Josh document flow hooks for suite-level lifecycle.
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
            processDocument(page, loginPage, documentSet, failures, testClass, testMethod, true, true);
        }

        failIfAny(failures, failureSummaryPrefix);
    }

    /**
     * Admin queries only — documents must already be uploaded (e.g. by flow setup).
     * Does not upload or delete.
     */
    public static void runAdminDocumentQueriesOnly(
            Page page,
            String testClass,
            String testMethod,
            String failureSummaryPrefix
    ) throws Exception {
        LoginPage loginPage = new LoginPage(page);
        List<DocumentQuerySet> documents = loadFilteredDocuments();
        List<String> failures = new ArrayList<>();

        for (DocumentQuerySet documentSet : documents) {
            processDocument(page, loginPage, documentSet, failures, testClass, testMethod, false, false);
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
     * Uploads and prepares every filtered document (removes any stale copy first).
     * Used by Josh flow {@code @BeforeGroups} setup.
     */
    public static void uploadAllFilteredDocuments(Page page) throws Exception {
        LoginPage loginPage = new LoginPage(page);
        for (DocumentQuerySet documentSet : loadFilteredDocuments()) {
            DocumentLifecycleManager.uploadAndPrepareDocument(page, loginPage, documentSet);
        }
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
            String testMethod,
            boolean upload,
            boolean delete
    ) throws Exception {
        List<QueryModel> queries = documentSet.getQueries();
        Assert.assertNotNull(queries, "queries missing for document: " + documentSet.getDocument());
        Assert.assertFalse(queries.isEmpty(), "queries empty for document: " + documentSet.getDocument());

        String documentTitle;
        if (upload) {
            documentTitle = DocumentLifecycleManager.uploadAndPrepareDocument(page, loginPage, documentSet);
        } else {
            documentTitle = documentSet.getDocument();
            Assert.assertNotNull(documentTitle, "document title missing");
            Assert.assertFalse(documentTitle.isBlank(), "document title blank");
        }

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
            if (delete) {
                DocumentLifecycleManager.deleteDocument(loginPage, documentTitle);
            }
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
