package com.supporthawk.tests.fintech.postlogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.config.TenantRoutes;
import com.supporthawk.fintech.customer.CustomerLoginHelper;
import com.supporthawk.fintech.customer.CustomerQueryDataProvider;
import com.supporthawk.fintech.customer.CustomerQueryRunner;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Post-login customer voice query tests using customer_queries.json.
 */
public class FintechCustomerLoginVoiceTest extends BasePage {

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
        queryPage.navigate(TenantRoutes.Tenant.FINTECH);
        CustomerQueryRunner.executeVoiceQuery(queryPage, queryModel);
    }
}
