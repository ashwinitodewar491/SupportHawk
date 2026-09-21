package com.supporthawk.tests.josh.prelogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.document.JoshDocumentTestRunner;
import org.testng.annotations.Test;

/**
 * Josh pre-login document text queries: open Josh /query with no login and ask
 * questions from document_queries.json against documents already uploaded by admin.
 */
public class JoshPreLoginDocumentTextTest extends BasePage {

    @Test(description = "Ask Josh pre-login document queries and verify responses/references")
    public void testJoshPreLoginDocumentTextQueries() {
        JoshDocumentTestRunner.runPreLoginDocumentQuerySuite(
                page,
                getClass().getSimpleName(),
                "testJoshPreLoginDocumentTextQueries",
                "Josh pre-login document text test completed with failures:\n"
        );
    }
}
