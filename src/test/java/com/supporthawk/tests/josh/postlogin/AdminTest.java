package com.supporthawk.tests.josh.postlogin;

import com.supporthawk.admin.AdminLoginHelper;
import com.supporthawk.base.BasePage;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh Admin post-login document flow driven by document_queries.json:
 * for each document — upload, preview, edit, query + references, then delete —
 * with Admin login performed once.
 *
 * Query-level response and document/reference failures are soft-recorded so
 * remaining queries and documents still run; the TestNG test fails once at the
 * end if any were recorded.
 */
public class AdminTest extends BasePage {

    @Test(description = "Upload each document from JSON, query it, verify references, then delete")
    public void testAdminDocumentUpload() throws Exception {
        AdminLoginHelper.loginAsAdmin(page);
        JoshDocumentTestRunner.runDocumentQuerySuite(
                page,
                getClass().getSimpleName(),
                "testAdminDocumentUpload",
                "Admin document test completed with failures:\n"
        );
    }
}
