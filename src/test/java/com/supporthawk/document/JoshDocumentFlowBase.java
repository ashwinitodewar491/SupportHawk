package com.supporthawk.document;

import com.supporthawk.base.BasePage;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared Josh document flow hooks for Admin + pre-login tests.
 *
 * <p>Guarantees (via TestNG groups + priorities on the concrete tests):
 * {@code BeforeGroups setup → AdminTest → JoshPreLoginDocumentTextTest → AfterGroups cleanup}.
 *
 * <p>Setup/cleanup use a dedicated admin Playwright session so they do not depend on
 * the per-method {@link BasePage} browser. Guards ensure hooks run once even when both
 * subclasses contribute the same configuration methods.
 */
public abstract class JoshDocumentFlowBase extends BasePage {

    /** TestNG group shared by Admin and Josh pre-login document tests. */
    public static final String JOSH_DOCUMENT_FLOW_GROUP = "josh-document-flow";

    private static final AtomicBoolean SETUP_DONE = new AtomicBoolean(false);
    private static final AtomicBoolean CLEANUP_DONE = new AtomicBoolean(false);

    @BeforeGroups(groups = JOSH_DOCUMENT_FLOW_GROUP, alwaysRun = true)
    public void setupJoshDocumentsForFlow() throws Exception {
        if (!SETUP_DONE.compareAndSet(false, true)) {
            return;
        }
        CLEANUP_DONE.set(false);
        System.out.println("[Josh Document Flow] Setup: admin upload of filtered documents");
        JoshDocumentAdminSession.withAdminPage(JoshDocumentTestRunner::uploadAllFilteredDocuments);
        System.out.println("[Josh Document Flow] Setup complete");
    }

    @AfterGroups(groups = JOSH_DOCUMENT_FLOW_GROUP, alwaysRun = true)
    public void cleanupJoshDocumentsForFlow() throws Exception {
        if (!CLEANUP_DONE.compareAndSet(false, true)) {
            return;
        }
        try {
            System.out.println("[Josh Document Flow] Cleanup: admin delete of filtered documents");
            JoshDocumentAdminSession.withAdminPage(JoshDocumentTestRunner::deleteAllFilteredDocuments);
            System.out.println("[Josh Document Flow] Cleanup complete");
        } finally {
            SETUP_DONE.set(false);
        }
    }
}
