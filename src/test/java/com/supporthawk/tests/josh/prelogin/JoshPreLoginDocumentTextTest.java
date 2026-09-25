package com.supporthawk.tests.josh.prelogin;

import com.supporthawk.document.JoshDocumentFlowBase;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh public/customer document text suite.
 * Relies on the shared Josh document flow: admin setup uploads the test document,
 * this class only asks questions (no login, upload, or delete), then flow cleanup
 * deletes the document after both Admin and pre-login tests finish.
 */
public class JoshPreLoginDocumentTextTest extends JoshDocumentFlowBase {

    @Test(
            groups = JoshDocumentFlowBase.JOSH_DOCUMENT_FLOW_GROUP,
            priority = 20,
            description = "Ask Josh pre-login document queries and verify responses/references"
    )
    public void testJoshPreLoginDocumentTextQueries() {
        JoshDocumentTestRunner.runPreLoginDocumentQuerySuite(
                page,
                getClass().getSimpleName(),
                "testJoshPreLoginDocumentTextQueries",
                "Josh pre-login document text test completed with failures:\n"
        );
    }
}
