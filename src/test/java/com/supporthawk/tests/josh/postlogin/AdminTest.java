package com.supporthawk.tests.josh.postlogin;

import com.supporthawk.admin.AdminLoginHelper;
import com.supporthawk.document.JoshDocumentFlowBase;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh Admin post-login document flow driven by document_queries.json:
 * upload, preview, edit, then query + references. Document is kept for
 * {@code JoshPreLoginDocumentTextTest}; suite {@code @AfterGroups} deletes it.
 *
 * <p>Query-level response and document/reference failures are soft-recorded so
 * remaining queries still run; the TestNG test fails once at the end if any were recorded.
 */
public class AdminTest extends JoshDocumentFlowBase {

    @Test(
            groups = JoshDocumentFlowBase.JOSH_DOCUMENT_FLOW_GROUP,
            priority = 10,
            description = "Upload each document from JSON, query it, verify references (keep for pre-login)"
    )
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
