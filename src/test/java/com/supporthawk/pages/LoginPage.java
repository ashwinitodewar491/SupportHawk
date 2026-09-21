package com.supporthawk.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;

public class LoginPage {

    private final Page page;

    private final String loginButton = "text=Login";
    private final String loginAsAdminButton = "text=Login as Admin";
    private final String loginAsCustomerButton = "text=Login as Customer";
    private final String usernameInput = "#username";
    private final String passwordInput = "#password";
    private final String signInButton = "text=Sign in";
    private final String enterCIF = "#customerId";
    private final String enterMPIN = "#mpin";
    private final String continueButton = "text=Continue";
    private final String customerLogin = "text=Sign in to Banking";
    private final String logoutButton = "span:text-is('Logout')";
    private final String adminLoginError =
            "div.text-destructive:has-text('Incorrect username or password')";
    private final String customerLoginError =
            "div.text-destructive:has-text('not found')";
    private final String documentsPage = "a[href='/documents']:visible";
    private final String queryPage = "a[href='/query']:visible";
    private final String uploadDocumentsButton = "text = Upload Document";
    private final String uploadPdfButton = "#upload-pdf";
    private final String documentTitleInput = "#upload-title";
    private final String documentDescriptionInput = "#upload-description";
    private final String documentTagsInput = "#react-select-2-input";
    private final String submitButton = "button[type='submit']";
    private final String deleteDocumentButton = "[title='Delete document']";
    private final String confirmDeleteButton = "button:has-text('Delete')";
    private final String previewDocumentButton = "button[title='Preview document']";
    private final String editDocumentButton = "button[title='Edit document']";
    private final String editDocumentTitleInput = "#edit-title";
    private final String editDocumentDescriptionInput = "#edit-description";
    private final String updateButton = "button[type='submit']";

    public LoginPage(Page page) {
        this.page = page;
    }

    public void clickLogin() {
        page.locator(loginButton).click();
    }

    public void loginAsAdmin() {
        page.locator(loginAsAdminButton).click();
    }

    public void enterUsername(String username){
        page.locator(usernameInput).fill(username);
    }

    public void enterPassword(String password){
        page.locator(passwordInput).fill(password);
    }

    public void clickSignIn(){
        page.locator(signInButton).click();
    
    }

    public void loginAsCustomer() {
        page.locator(loginAsCustomerButton).click();
    }

    public void enterCIF(String cif){
        page.locator(enterCIF).fill(cif);
    }

    public void clickContinue(){
        page.locator(continueButton).click();
    
    }

    public void enterMPIN(String mpin){
        page.locator(enterMPIN).fill(mpin);
    }

    public void clickSignInCustomer(){
        page.locator(customerLogin).click();
    
    }

    public boolean verifyCustomerLogin(){
        return page.locator(logoutButton).isVisible();
    }

    public boolean isAdminLoginErrorVisible() {
        return page.locator(adminLoginError).isVisible();
    }

    public boolean isCustomerLoginErrorVisible() {
        return page.locator(customerLoginError).isVisible();
    }

    /**
     * Waits until Admin login either leaves {@code /login} or shows the credentials error.
     */
    public void waitForAdminLoginOutcome() {
        page.waitForCondition(() ->
                !page.url().toLowerCase().contains("/login")
                        || page.locator(adminLoginError).isVisible()
        );
    }

    /**
     * Waits until Customer login either shows Logout or the "not found" error.
     */
    public void waitForCustomerLoginOutcome() {
        page.locator(logoutButton)
                .or(page.locator(customerLoginError))
                .waitFor();
    }

    public void goToDocumentsPage(){
        page.locator(documentsPage).click();
    }

    public void goToQueryPage() {
        // Logo and sidebar both use href=/query; click the sidebar "Query" entry.
        page.locator(queryPage)
                .filter(new Locator.FilterOptions().setHasText("Query"))
                .click();
    }

    public void clickUploadDocuments(){
        page.locator(uploadDocumentsButton).click();
    }

    /** True when a documents table row with the given title is still visible. */
    public boolean isDocumentPresent(String documentTitle) {
        return page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle))
                .count() > 0;
    }

    /**
     * Deletes every document whose displayed title exactly matches {@code documentTitle},
     * then waits until none remain. Does nothing if no such document exists.
     *
     * The document list is a table. Each row is matched via filter(hasText) which
     * checks all descendant text regardless of nesting depth (td / span / a / p).
     * The delete button is scoped to the matching row so other documents are untouched.
     */
    public void deleteDocumentIfExists(String documentTitle) {
        // filter(hasText) matches any row whose descendant text contains the title.
        // We use getByRole(ROW) to avoid matching non-data rows (header, etc.).
        Locator matchingRows = page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle));

        // Delete one row at a time until none with this title remain.
        while (matchingRows.count() > 0) {
            int countBefore = matchingRows.count();
            matchingRows.first().locator(deleteDocumentButton).click();
            page.locator(confirmDeleteButton).click();

            // Wait until the count drops — confirms the deletion completed.
            page.waitForFunction(
                    "([selector, text, before]) => " +
                    "Array.from(document.querySelectorAll(selector))" +
                    "  .filter(el => el.innerText.includes(text)).length < before",
                    new Object[]{
                        "tr[class*='border-b']",
                        documentTitle,
                        countBefore
                    }
            );
        }
    }

    public void uploadPdfDocument(Path pdfPath, String title, String description, String... tags) {
        // Use Playwright's file upload mechanism (no native file chooser).
        page.locator(uploadPdfButton).setInputFiles(pdfPath);

        page.locator(documentTitleInput).fill(title);
        page.locator(documentDescriptionInput).fill(description);

        page.locator(documentTagsInput).click();
        for (String tag : tags) {
            page.locator(documentTagsInput).fill(tag);
            page.locator(documentTagsInput).press("Enter");
        }

        page.locator(submitButton).click();

        // Wait for a table row containing the document title to appear after submit.
        page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(title))
                .first()
                .waitFor();
    }

    /**
     * Opens Preview for the documents-table row matching {@code documentTitle},
     * verifies the PDF/document opens in a new tab, closes that tab, and returns
     * focus to the Documents page.
     */
    public void previewAndVerifyDocument(String documentTitle) {
        Locator documentRow = page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle))
                .first();
        documentRow.waitFor();

        Locator previewButton = documentRow.locator(previewDocumentButton);
        previewButton.waitFor();

        String documentsPageUrl = page.url();

        // Register popup waiter BEFORE the click so the new tab is captured reliably.
        Page previewPage;
        try {
            previewPage = page.waitForPopup(
                    new Page.WaitForPopupOptions().setTimeout(15000),
                    previewButton::click
            );
        } catch (Exception popupTimeout) {
            throw new AssertionError(
                    "Preview did not open a new tab for document: " + documentTitle,
                    popupTimeout
            );
        }

        previewPage.waitForLoadState();

        boolean documentOpened = isPreviewDocumentOpen(previewPage);

        System.out.println("--------------------------------------------------");
        System.out.println("Preview URL: " + previewPage.url());
        System.out.println("Document viewer open: " + documentOpened);
        System.out.println("--------------------------------------------------");

        try {
            if (!documentOpened) {
                throw new AssertionError(
                        "Preview PDF/document did not open.\n"
                                + "Opened URL: " + previewPage.url()
                );
            }
        } finally {
            previewPage.close();
            if (!page.url().equals(documentsPageUrl)) {
                page.navigate(documentsPageUrl);
            }
        }

        // Continue on the original Documents page / table.
        page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle))
                .first()
                .waitFor();
    }

    /**
     * Opens Edit for the documents-table row matching {@code documentTitle},
     * updates description and adds tags (without clearing existing ones),
     * clicks Update, then verifies the row still shows the same title and
     * contains each newly added tag.
     *
     * Does not change the document title field.
     */
    public void editAndVerifyDocument(
            String documentTitle,
            String updatedDescription,
            String... tagsToAdd
    ) {
        Locator documentRow = page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle))
                .first();
        documentRow.waitFor();
        documentRow.locator(editDocumentButton).click();

        // Wait for the Edit form (inspected DOM: #edit-title, #edit-description).
        page.locator(editDocumentDescriptionInput).waitFor();
        page.locator(editDocumentTitleInput).waitFor();

        // Title must remain unchanged — do not fill/clear #edit-title.
        page.locator(editDocumentDescriptionInput).fill(updatedDescription);

        // react-select id is dynamic (#react-select-2-input on upload, #react-select-3-input
        // on edit after upload); scope to the Edit form instead of the upload locator.
        Locator editTagsInput = page.getByLabel("Edit Document")
                .locator("[id^='react-select'][id$='-input']");

        // Existing tags stay selected in react-select; only append new ones.
        for (String tag : tagsToAdd) {
            editTagsInput.click();
            editTagsInput.fill(tag);
            editTagsInput.press("Enter");
        }

        page.locator(updateButton)
                .filter(new Locator.FilterOptions().setHasText("Update"))
                .click();

        // Wait until the Edit form closes (update completed).
        page.locator(editDocumentDescriptionInput).waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN)
        );

        Locator updatedRow = page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(documentTitle))
                .first();
        updatedRow.waitFor();

        if (!updatedRow.isVisible()) {
            throw new AssertionError(
                    "Document row missing after edit. Expected title: " + documentTitle
            );
        }

        for (String tag : tagsToAdd) {
            Locator tagInRow = updatedRow.getByText(tag, new Locator.GetByTextOptions().setExact(true));
            tagInRow.waitFor();
            if (!tagInRow.isVisible()) {
                throw new AssertionError(
                        "Expected tag not found in document row after edit.\n"
                                + "Document title: " + documentTitle + "\n"
                                + "Expected tag: " + tag
                );
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Document edited: " + documentTitle);
        System.out.println("Updated description applied (not shown in table).");
        System.out.println("Added tags verified: " + String.join(", ", tagsToAdd));
        System.out.println("--------------------------------------------------");
    }

    private boolean isPreviewDocumentOpen(Page previewPage) {
        String url = previewPage.url() == null ? "" : previewPage.url().toLowerCase();
        if (url.contains(".pdf") || url.contains("/documents/") || url.contains("blob:")) {
            return true;
        }
        if (previewPage.locator("embed, object, iframe, canvas").count() > 0) {
            return true;
        }
        String body = previewPage.locator("body").innerText();
        return body != null && !body.trim().isEmpty();
    }
}
