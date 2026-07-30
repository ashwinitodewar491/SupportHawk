package com.supporthawk.tests;

import com.supporthawk.pages.BasePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleTest extends BasePage {

    @Test(groups = "smoke", description = "Playwright home page loads with expected title")
    public void hasCorrectTitle() {
        page.navigate("https://playwright.dev");
        Assert.assertTrue(page.title().contains("Playwright"));
    }
}
