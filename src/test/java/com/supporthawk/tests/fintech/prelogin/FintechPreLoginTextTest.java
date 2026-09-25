package com.supporthawk.tests.fintech.prelogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryModel;
import com.supporthawk.fintech.prelogin.FintechPreLoginQueryRunner;
import com.supporthawk.fintech.prelogin.FintechPreLoginQueryDataProvider;
import com.supporthawk.pages.QueryPage;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/* Data-driven UI test for the SupportHawk Query page.
 * Each row from fintech_queries.json becomes one TestNG test run.*/
public class FintechPreLoginTextTest extends BasePage implements ITest {

    private final ThreadLocal<String> testName = new ThreadLocal<>();

    @Override
    public String getTestName() {
        String name = testName.get();
        return name != null ? name : "verifyQueryResponse";
    }

    /** Sets Surefire/TestNG invocation name to methodName(actual query text). */
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

    /**
     * Asks each question from fintech_queries.json and checks that enough
     * expected keywords appear in the AI response (case-insensitive).
     *
     * Matching rules:
     * - 1 expected keyword  → need 1 match
     * - 2 expected keywords → need 2 matches
     * - 3 expected keywords → need 2 matches
     * - 4+ expected keywords → need 3 matches
     *
     * @param queryModel one query + expected keywords from the DataProvider
     */
    @Test(
            dataProvider = "queryData",
            dataProviderClass = FintechPreLoginQueryDataProvider.class,
            description = "Verify Query page responses contain enough expected keywords"
    )
    public void verifyQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);
        FintechPreLoginQueryRunner.executeTextQuery(
                queryPage,
                queryModel,
                getClass().getSimpleName(),
                "verifyQueryResponse"
        );
    }
}
