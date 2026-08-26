package com.supporthawk.tests.prelogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.config.ConfigReader;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.supporthawk.utils.KeywordValidator;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

/* Data-driven UI test for the SupportHawk Query page.
 * Each row from fintech_queries.json becomes one TestNG test run.*/
public class SupportHawkPreLoginQueryTest extends BasePage implements ITest {

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
     * DataProvider that reads queries from fintech_queries.json and filters
     * them based on the Maven property -DtestGroups.
     *
     * How Maven passes testGroups:
     *   mvn test -DtestGroups=smoke
     *   mvn test -DtestGroups=regression
     * Maven puts that value into System.getProperty("testGroups").
     *
     * Filtering rules (multi-tag support):
     * - smoke      → run queries whose tags list CONTAINS "smoke"
     * - regression → run queries whose tags list CONTAINS "regression"
     * - (no value) → run ALL queries
     *
     * A query can have several tags, e.g. ["smoke", "regression"],
     * so the same query can run in both suites.
     *
     * Each row is: { QueryModel }
     */
    @DataProvider(name = "queryData")
    public Object[][] queryData() {
        // Choose the query file to load for this suite.
        // You can replace this with another file like "joshsoftware_queries.json".
        String queryFileName = ConfigReader.get("query.file");

        // Load every query from the JSON file
        List<QueryModel> allQueries = QueryData.getQueries(queryFileName);

        // Read the suite name from Maven: -DtestGroups=smoke or -DtestGroups=regression
        // If nothing was passed, this will be null and we keep all queries.
        String testGroups = System.getProperty("testGroups");

        // Build the list of queries that should actually run
        List<QueryModel> selectedQueries = new ArrayList<>();

        if (testGroups == null || testGroups.trim().isEmpty()) {
            // No filter requested — run every query
            selectedQueries = allQueries;
        } else {
            // Keep only queries that have this suite name in their tags list
            for (int i = 0; i < allQueries.size(); i++) {
                QueryModel query = allQueries.get(i);
                if (hasTag(query, testGroups)) {
                    selectedQueries.add(query);
                }
            }
        }

        // TestNG DataProviders need Object[][] — one row per selected query
        Object[][] data = new Object[selectedQueries.size()][1];
        for (int i = 0; i < selectedQueries.size(); i++) {
            data[i][0] = selectedQueries.get(i);
        }
        return data;
    }

    /**
     * Returns true if the query's tags list contains the given suite name
     * (case-insensitive). Used by the DataProvider filter.
     */
    private boolean hasTag(QueryModel query, String suiteName) {
        List<String> tags = query.getTags();
        if (tags == null) {
            return false;
        }
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            if (tag != null && tag.equalsIgnoreCase(suiteName)) {
                return true;
            }
        }
        return false;
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
    @Test(dataProvider = "queryData",
            description = "Verify Query page responses contain enough expected keywords")
    public void verifyQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);

        queryPage.navigate();
        String response = queryPage.askQuestion(queryModel.getQuery());

        List<String> expectedKeywords = queryModel.getExpected();

        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        int totalMatched = matchedKeywords.size();
        boolean keywordValidationPassed = totalMatched >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("BOT RESPONSE KEYWORD VALIDATION");
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Required matches: " + requiredMatches);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Matched count: " + totalMatched);
        System.out.println("Keyword validation: " + (keywordValidationPassed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        Assert.assertTrue(
                keywordValidationPassed,
                "Not enough keywords matched in BOT RESPONSE. Needed at least " + requiredMatches
                        + " but found " + totalMatched
                        + ". Expected: " + expectedKeywords
                        + ". Matched: " + matchedKeywords
        );

        queryPage.provideRandomFeedback("This is automated test feedback for thumbs down.");

        page.waitForTimeout(10000);
        queryPage.validateReferenceLinks(expectedKeywords);

    }
}
