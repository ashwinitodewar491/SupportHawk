// BAD — same test as good-test.java, with common violations called out inline.
// Do not generate code like this.
package com.supporthawk.tests;

import org.junit.Test; // VIOLATION: JUnit is banned — use TestNG's org.testng.annotations.Test
import org.junit.Assert;
import com.microsoft.playwright.Page;

public class CheckboxesTest {

    Page page; // VIOLATION: no BasePage lifecycle — browser is never actually launched here

    @Test // VIOLATION: no groups — TestNG group filtering (smoke/regression) can't select this test
    public void test1() { // VIOLATION: method name doesn't describe what's verified
        // VIOLATION: hardcoded host — should come from AppConfig.BASE_URL
        page.navigate("https://the-internet.herokuapp.com/checkboxes");

        // VIOLATION: locator inline in the test class instead of a page object
        page.locator("form#checkboxes input:nth-child(1)").click();

        // VIOLATION: fixed sleep instead of a real wait condition
        page.waitForTimeout(3000);

        // VIOLATION: no assertion at all — the test can never actually fail
    }
}
