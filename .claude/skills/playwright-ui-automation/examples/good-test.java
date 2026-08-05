// GOOD — follows every convention in dos-and-donts.md
package com.supporthawk.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class CheckboxesPage {

    private final Page page;

    public CheckboxesPage(Page page) {
        this.page = page;
    }

    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/checkboxes");
    }

    public void toggleCheckbox(int index) {
        page.getByRole(AriaRole.CHECKBOX).nth(index).click();
    }

    public boolean isCheckboxChecked(int index) {
        return page.getByRole(AriaRole.CHECKBOX).nth(index).isChecked();
    }
}

// --- test class ---

package com.supporthawk.tests;

import com.supporthawk.base.BasePage;
import com.supporthawk.config.AppConfig;
import com.supporthawk.pages.CheckboxesPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CheckboxesTest extends BasePage {

    private CheckboxesPage navigateToCheckboxesPage() {
        CheckboxesPage checkboxesPage = new CheckboxesPage(page);
        checkboxesPage.navigate(AppConfig.BASE_URL);
        return checkboxesPage;
    }

    @Test(groups = {"smoke", "regression", "ui"}, description = "Verify toggling a checkbox updates its checked state")
    public void testToggleCheckbox() {
        CheckboxesPage checkboxesPage = navigateToCheckboxesPage();
        boolean before = checkboxesPage.isCheckboxChecked(0);

        checkboxesPage.toggleCheckbox(0);

        Assert.assertNotEquals(checkboxesPage.isCheckboxChecked(0), before,
                "Checkbox state should flip after toggling");
    }
}
