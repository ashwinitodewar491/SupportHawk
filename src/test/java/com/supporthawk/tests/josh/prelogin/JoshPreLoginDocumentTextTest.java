package com.supporthawk.tests.josh.prelogin;

import com.supporthawk.document.JoshDocumentFlowBase;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh public/customer document text suite.
 * Uses the document uploaded by {@code AdminTest} (no login, upload, or delete).
 * Suite {@code @AfterGroups} deletes the document after both tests finish.
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
