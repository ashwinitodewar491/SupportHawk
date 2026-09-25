package com.supporthawk.document;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.supporthawk.admin.AdminLoginHelper;
import com.supporthawk.config.AppConfig;

/**
 * Short-lived Playwright session for Josh admin document <strong>cleanup</strong> only.
 * Independent of {@code BasePage} so {@code @AfterGroups} can delete without the
 * per-test browser. Not used for upload (AdminTest owns upload).
 */
public final class JoshDocumentAdminSession {

    @FunctionalInterface
    public interface PageAction {
        void run(Page page) throws Exception;
    }

    private JoshDocumentAdminSession() {
    }

    /**
     * Launches a browser matching {@link AppConfig}, logs in as admin,
     * runs {@code action}, then closes the browser.
     */
    public static void withAdminPage(PageAction action) throws Exception {
        Playwright playwright = null;
        Browser browser = null;
        try {
            playwright = Playwright.create();
            boolean headless = AppConfig.isHeadless();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless)
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            AdminLoginHelper.loginAsAdmin(page);
            action.run(page);
        } finally {
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        }
    }
}
