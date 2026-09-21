package com.supporthawk.customer;

import com.microsoft.playwright.Page;
import com.supporthawk.config.AppConfig;
import com.supporthawk.config.ConfigReader;
import com.supporthawk.pages.LoginPage;
import org.testng.Assert;

/**
 * Shared customer login steps for post-login query tests.
 */
public final class CustomerLoginHelper {

    private CustomerLoginHelper() {
    }

    public static void login(Page page) {
        LoginPage loginPage = new LoginPage(page);

        page.navigate(AppConfig.BASE_URL);
        loginPage.clickLogin();
        loginPage.loginAsCustomer();
        loginPage.enterCIF(ConfigReader.get("cif"));
        loginPage.clickContinue();
        loginPage.enterMPIN(ConfigReader.get("mpin"));
        loginPage.clickSignInCustomer();

        loginPage.waitForCustomerLoginOutcome();
        if (loginPage.isCustomerLoginErrorVisible()) {
            Assert.fail("Customer login failed: user not found");
        }

        Assert.assertTrue(
                loginPage.verifyCustomerLogin(),
                "Customer login failed — Logout control is not visible after Sign in"
        );
    }
}
