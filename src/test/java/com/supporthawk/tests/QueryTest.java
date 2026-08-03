package com.supporthawk.tests;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven UI test for the SupportHawk Query page.
 * Each row from fintech_queries.json becomes one TestNG test run.
 */
public class QueryTest extends BasePage {

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
    @Test(dataProvider = "queryData", groups = {"smoke", "regression", "ui"},
            description = "Verify Query page responses contain enough expected keywords")
    public void verifyQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);

        // Open the Query page
        queryPage.navigate();

        // Ask the question and wait for the AI reply
        String response = queryPage.askQuestion(queryModel.getQuery());

        // Get the list of expected keywords from the JSON
        List<String> expectedKeywords = queryModel.getExpected();

        // Compare using lowercase so "Savings" and "savings" both count as a match
        String responseLower = response.toLowerCase();

        // Collect every keyword that was actually found in the response
        List<String> matchedKeywords = new ArrayList<>();
        for (int i = 0; i < expectedKeywords.size(); i++) {
            String keyword = expectedKeywords.get(i);
            if (responseLower.contains(keyword.toLowerCase())) {
                matchedKeywords.add(keyword);
            }
        }

        // Decide how many matches are required:
        // - one keyword in JSON  → must find that 1
        // - multiple keywords    → must find at least 2
        int requiredMatches;
        if (expectedKeywords.size() == 1) {
            requiredMatches = 1;
        } else {
            requiredMatches = 2;
        }

        int totalMatched = matchedKeywords.size();
        int totalExpected = expectedKeywords.size();

        // Print a clear debug block so failures are easy to investigate
        System.out.println("--------------------------------------------------");
        System.out.println("Question:");
        System.out.println(queryModel.getQuery());
        System.out.println();
        System.out.println("Expected keywords:");
        System.out.println(expectedKeywords);
        System.out.println();
        System.out.println("Matched keywords:");
        System.out.println(matchedKeywords);
        System.out.println();
        System.out.println("Total matched:");
        System.out.println(totalMatched + " of " + totalExpected);
        System.out.println();
        System.out.println("Response:");
        System.out.println(response);
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
