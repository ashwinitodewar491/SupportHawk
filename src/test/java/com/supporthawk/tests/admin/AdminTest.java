package com.supporthawk.tests.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.supporthawk.admin.AdminLoginHelper;
import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.LoginPage;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.utils.KeywordValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * End-to-end Admin flow: upload a document, ask document queries, verify
 * references, then delete the uploaded document.
 */
public class AdminTest extends BasePage {

    @Test(description = "Upload document, query it, verify references, then delete")
    public void testAdminDocumentUpload() throws Exception {
        AdminLoginHelper.loginAsAdmin(page);

        String documentTitle = "Josh Software Organization";
        String description = "Test document upload for Josh Software organization";
        String[] tags = new String[] {"Josh Software", "Company Details"};

        LoginPage loginPage = new LoginPage(page);
        loginPage.goToDocumentsPage();

        // Remove a previously uploaded copy of this document if one exists,
        // so the upload always starts from a clean state.
        loginPage.deleteDocumentIfExists(documentTitle);

        loginPage.clickUploadDocuments();

        Path pdfPath = loadPdfFromTestResources(
                "Documents/Josh Software Organization.pdf"
        );

        loginPage.uploadPdfDocument(pdfPath, documentTitle, description, tags);

        Assert.assertTrue(
                page.getByRole(AriaRole.ROW)
                        .filter(new Locator.FilterOptions().setHasText(documentTitle))
                        .first()
                        .isVisible(),
                "Expected uploaded document title to appear in the documents UI"
        );

        // Preview the uploaded document from its table row, then continue on Documents.
        loginPage.previewAndVerifyDocument(documentTitle);

        // Edit description + add Automation tag (title and existing tags unchanged).
        String updatedDescription = "Updated description for Josh Software organization";
        loginPage.editAndVerifyDocument(documentTitle, updatedDescription, "Automation");

        // --- Query the uploaded document and verify references ---
        loginPage.goToQueryPage();
        QueryPage queryPage = new QueryPage(page);

        List<QueryModel> queries = QueryData.getQueriesForDocument(
                "document_queries.json",
                documentTitle
        );

        for (QueryModel queryModel : queries) {
            String response = queryPage.askQuestion(queryModel.getQuery());
            validateDocumentQueryResponse(documentTitle, queryModel, response);
            queryPage.openAndVerifyLatestDocumentReference(documentTitle);
        }

        // --- Cleanup: delete the uploaded document ---
        loginPage.goToDocumentsPage();
        loginPage.deleteDocumentIfExists(documentTitle);
        Assert.assertFalse(
                loginPage.isDocumentPresent(documentTitle),
                "Document should no longer exist after deletion: " + documentTitle
        );
    }

    private static void validateDocumentQueryResponse(
            String documentTitle,
            QueryModel queryModel,
            String response
    ) {
        List<String> expectedKeywords = queryModel.getExpected();
        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        boolean passed = matchedKeywords.size() >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("Document: " + documentTitle);
        System.out.println("Query: " + queryModel.getQuery());
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Keyword validation: " + (passed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        Assert.assertTrue(
                passed,
                "Document: " + documentTitle + "\n"
                        + "Query: " + queryModel.getQuery() + "\n"
                        + "Expected keywords: " + expectedKeywords + "\n"
                        + "Actual response: " + response
        );
    }

    private static Path loadPdfFromTestResources(String resourcePath) throws Exception {
        try (InputStream inputStream = AdminTest.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not find test resource: " + resourcePath);
            }

            Path tempFile = Files.createTempFile("supporthawk-", ".pdf");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }
}
