package com.supporthawk.document;

import com.supporthawk.base.BasePage;
import org.testng.annotations.AfterGroups;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared Josh document flow hooks for Admin + pre-login tests.
 *
 * <p>Order (via TestNG groups + priorities + suite XML):
 * {@code AdminTest (upload + queries, keep doc) → JoshPreLoginDocumentTextTest → AfterGroups cleanup}.
 *
 * <p>Upload stays in {@code AdminTest}. This base only deletes after both tests finish,
 * using a dedicated admin Playwright session (independent of per-test {@link BasePage}).
 */
public abstract class JoshDocumentFlowBase extends BasePage {

    /** TestNG group shared by Admin and Josh pre-login document tests. */
    public static final String JOSH_DOCUMENT_FLOW_GROUP = "josh-document-flow";

    private static final AtomicBoolean CLEANUP_DONE = new AtomicBoolean(false);

    @AfterGroups(groups = JOSH_DOCUMENT_FLOW_GROUP, alwaysRun = true)
    public void cleanupJoshDocumentsForFlow() throws Exception {
        if (!CLEANUP_DONE.compareAndSet(false, true)) {
            return;
        }
        System.out.println("[Josh Document Flow] Cleanup: admin delete of filtered documents");
        JoshDocumentAdminSession.withAdminPage(JoshDocumentTestRunner::deleteAllFilteredDocuments);
        System.out.println("[Josh Document Flow] Cleanup complete");
    }
}
