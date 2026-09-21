package com.supporthawk.tests.postlogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.customer.CustomerLoginHelper;
import com.supporthawk.customer.CustomerQueryDataProvider;
import com.supporthawk.customer.CustomerQueryRunner;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Post-login customer voice query tests using customer_queries.json.
 */
public class CustomerLoginVoiceTest extends BasePage {

    @BeforeMethod(dependsOnMethods = "setUpBrowser", alwaysRun = true)
    public void loginAsCustomer() {
        CustomerLoginHelper.login(page);
    }

    @DataProvider(name = "queryData")
    public Object[][] queryData() {
        return CustomerQueryDataProvider.loadFilteredQueries();
    }

    @Test(
            dataProvider = "queryData",
            description = "Verify post-login customer voice query responses contain enough expected keywords"
    )
    public void verifyCustomerVoiceQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);
        queryPage.navigate();
        CustomerQueryRunner.executeVoiceQuery(queryPage, queryModel);
    }
}
