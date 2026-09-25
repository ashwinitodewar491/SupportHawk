package com.supporthawk.tests.josh.postlogin;

import com.supporthawk.admin.AdminLoginHelper;
import com.supporthawk.document.JoshDocumentFlowBase;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh Admin post-login document query suite.
 *
 * <p>Document upload/delete is owned by {@link JoshDocumentFlowBase} so one automation
 * document is shared with {@code JoshPreLoginDocumentTextTest}:
 * setup upload → this admin query run → pre-login queries → cleanup delete.
 *
 * <p>Query-level response and document/reference failures are soft-recorded so
 * remaining queries still run; the TestNG test fails once at the end if any were recorded.
 */
public class AdminTest extends JoshDocumentFlowBase {

    @Test(
            groups = JoshDocumentFlowBase.JOSH_DOCUMENT_FLOW_GROUP,
            priority = 10,
            description = "Admin Josh document queries (document already uploaded by flow setup)"
    )
    public void testAdminDocumentUpload() throws Exception {
        AdminLoginHelper.loginAsAdmin(page);
        JoshDocumentTestRunner.runAdminDocumentQueriesOnly(
                page,
                getClass().getSimpleName(),
                "testAdminDocumentUpload",
                "Admin document test completed with failures:\n"
        );
    }
}
