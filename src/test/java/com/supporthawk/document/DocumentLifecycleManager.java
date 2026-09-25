package com.supporthawk.document;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.supporthawk.data.DocumentQuerySet;
import com.supporthawk.pages.LoginPage;
import org.testng.Assert;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Upload, verify, and delete lifecycle for Admin document tests.
 * Preserves the previous AdminTest document setup/cleanup behavior.
 */
public final class DocumentLifecycleManager {

    private DocumentLifecycleManager() {
    }

    /**
     * Goes to Documents, removes any existing copy, uploads the PDF from test
     * resources, asserts the title row is visible, then preview- and edit-verifies.
     *
     * @return the non-blank document title from {@code documentSet}
     */
    public static String uploadAndPrepareDocument(
            Page page,
            LoginPage loginPage,
            DocumentQuerySet documentSet
    ) throws Exception {
        String documentTitle = requireNonBlank(documentSet.getDocument(), "document");
        String description = requireNonBlank(documentSet.getDescription(), "description");
        String pdfResource = requireNonBlank(documentSet.getPdf(), "pdf");
        List<String> tags = documentSet.getTags();

        Assert.assertNotNull(tags, "tags missing for document: " + documentTitle);
        Assert.assertFalse(tags.isEmpty(), "tags empty for document: " + documentTitle);

        String[] tagArray = tags.toArray(new String[0]);
        String updatedDescription = "Updated description for " + documentTitle;

        loginPage.goToDocumentsPage();

        // Remove a previously uploaded copy so upload starts from a clean state.
        loginPage.deleteDocumentIfExists(documentTitle);

        loginPage.clickUploadDocuments();
        Path pdfPath = loadPdfFromTestResources(pdfResource);
        loginPage.uploadPdfDocument(pdfPath, documentTitle, description, tagArray);

        Assert.assertTrue(
                page.getByRole(AriaRole.ROW)
                        .filter(new Locator.FilterOptions().setHasText(documentTitle))
                        .first()
                        .isVisible(),
                "Expected uploaded document title to appear in the documents UI: " + documentTitle
        );

        loginPage.previewAndVerifyDocument(documentTitle);
        loginPage.editAndVerifyDocument(documentTitle, updatedDescription, "Automation");

        return documentTitle;
    }

    /**
     * Returns to Documents, deletes the document if present, and asserts it is gone.
     */
    public static void deleteDocument(LoginPage loginPage, String documentTitle) {
        loginPage.goToDocumentsPage();
        loginPage.deleteDocumentIfExists(documentTitle);
        Assert.assertFalse(
                loginPage.isDocumentPresent(documentTitle),
                "Document should no longer exist after deletion: " + documentTitle
        );
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or blank '" + fieldName + "' in document_queries.json entry"
            );
        }
        return value;
    }

    private static Path loadPdfFromTestResources(String resourcePath) throws Exception {
        try (InputStream inputStream = DocumentLifecycleManager.class
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
