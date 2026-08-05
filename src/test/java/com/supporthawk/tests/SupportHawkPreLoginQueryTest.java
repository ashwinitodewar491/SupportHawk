package com.supporthawk.tests;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.supporthawk.utils.KeywordValidator;
import java.util.List;
import java.util.ArrayList;

/**
 * Data-driven UI test for the SupportHawk Query page.
 * Each row from fintech_queries.json becomes one TestNG test run.
 */
public class SupportHawkPreLoginQueryTest extends BasePage {

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
        // Load every query from the JSON file
        List<QueryModel> allQueries = QueryData.getQueries();

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
     * - 2+ expected keywords → need at least 2 matches
     *
     * @param queryModel one query + expected keywords from the DataProvider
     */
    @Test(dataProvider = "queryData",
            description = "Verify Query page responses contain enough expected keywords")
    public void verifyQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);

        // Open the Query page
        queryPage.navigate();

        // Ask the question and wait for the AI reply
        String response = queryPage.askQuestion(queryModel.getQuery());

        // Get the list of expected keywords from the JSON
        List<String> expectedKeywords = queryModel.getExpected();

        List<String> matchedKeywords =
        KeywordValidator.findMatchedKeywords(response, expectedKeywords);

        int requiredMatches = KeywordValidator.findMatchedKeywords(response, expectedKeywords).size();

        int totalMatched = matchedKeywords.size();
        int totalExpected = expectedKeywords.size();

        // Print a clear debug block so failures are easy to investigate
        System.out.println("--------------------------------------------------");
        System.out.println("Query: " + queryModel.getQuery());
        System.out.println("Expected: " + expectedKeywords);
        System.out.println("Matched: " + matchedKeywords);
        System.out.println("Match count: " + totalMatched + "/" + totalExpected);
        System.out.println("Response: " + response);
        System.out.println("--------------------------------------------------");

        // Pass only if we found enough keywords
        Assert.assertTrue(
                totalMatched >= requiredMatches,
                "Not enough keywords matched. Needed at least " + requiredMatches
                        + " but found " + totalMatched
                        + ". Expected: " + expectedKeywords
                        + ". Matched: " + matchedKeywords
                        + ". Response: " + response
        );
    }
}
