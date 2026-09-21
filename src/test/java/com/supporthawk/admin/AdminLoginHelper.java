package com.supporthawk.admin;

import com.microsoft.playwright.Page;
import com.supporthawk.config.AppConfig;
import com.supporthawk.config.ConfigReader;
import com.supporthawk.config.TenantRoutes;
import com.supporthawk.pages.LoginPage;
import org.testng.Assert;

/**
 * Reusable Admin login steps extracted from {@code AdminTest}.
 * Keeps the exact existing login behavior and validations.
 * Enters via Josh {@code /query} ({@code base.url} + {@link TenantRoutes}).
 */
public final class AdminLoginHelper {

    private AdminLoginHelper() {
    }

    public static void loginAsAdmin(Page page) {
        LoginPage loginPage = new LoginPage(page);

        page.navigate(AppConfig.BASE_URL + TenantRoutes.queryPath(TenantRoutes.Tenant.JOSH));
        loginPage.clickLogin();
        loginPage.loginAsAdmin();
        loginPage.enterUsername(ConfigReader.get("username"));
        loginPage.enterPassword(ConfigReader.get("password"));
        loginPage.clickSignIn();

        loginPage.waitForAdminLoginOutcome();
        if (loginPage.isAdminLoginErrorVisible()) {
            Assert.fail("Admin login failed: Incorrect username or password");
        }

        Assert.assertFalse(
                page.locator("#username").isVisible(),
                "Admin login failed — username field is still visible after Sign in"
        );
        Assert.assertFalse(
                page.url().toLowerCase().contains("login"),
                "Admin login failed — still on a login URL: " + page.url()
        );
    }
}

