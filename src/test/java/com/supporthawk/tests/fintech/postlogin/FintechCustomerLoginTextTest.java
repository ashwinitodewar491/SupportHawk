package com.supporthawk.tests.fintech.postlogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.config.TenantRoutes;
import com.supporthawk.fintech.customer.CustomerLoginHelper;
import com.supporthawk.fintech.customer.CustomerQueryDataProvider;
import com.supporthawk.fintech.customer.CustomerQueryRunner;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * Post-login customer text query tests using customer_queries.json.
 */
public class FintechCustomerLoginTextTest extends BasePage implements ITest {

    private final ThreadLocal<String> testName = new ThreadLocal<>();

    @Override
    public String getTestName() {
        String name = testName.get();
        return name != null ? name : "verifyCustomerQueryResponse";
    }

    @BeforeMethod(alwaysRun = true)
    public void setInvocationName(Method method, Object[] params, ITestResult result) {
        String name;
        if (params != null && params.length > 0 && params[0] instanceof QueryModel) {
            QueryModel queryModel = (QueryModel) params[0];
            name = method.getName() + "(" + queryModel.getQuery() + ")";
        } else {
            name = method.getName();
        }
        testName.set(name);
        result.setTestName(name);
    }

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
            description = "Verify post-login customer text query responses contain enough expected keywords"
    )
    public void verifyCustomerQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);
        queryPage.navigate(TenantRoutes.Tenant.FINTECH);
        CustomerQueryRunner.executeTextQuery(queryPage, queryModel);
    }
}
