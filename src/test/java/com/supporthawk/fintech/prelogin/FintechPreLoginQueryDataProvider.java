package com.supporthawk.fintech.prelogin;

import com.supporthawk.config.ConfigReader;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.data.QueryTagFilter;
import org.testng.annotations.DataProvider;

import java.util.List;

/**
 * Shared DataProvider for Fintech pre-login text and voice crawl-query tests.
 * Loads {@code query.file} (typically fintech_queries.json) and filters by
 * {@code -DtestGroups} via {@link QueryTagFilter}.
 */
public final class FintechPreLoginQueryDataProvider {

    private FintechPreLoginQueryDataProvider() {
    }

    /**
     * DataProvider that reads queries from the configured query file and filters
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
    public static Object[][] queryData() {
        String queryFileName = ConfigReader.get("query.file");
        List<QueryModel> selectedQueries = QueryTagFilter.filterQueries(
                QueryData.getQueries(queryFileName)
        );

        Object[][] data = new Object[selectedQueries.size()][1];
        for (int i = 0; i < selectedQueries.size(); i++) {
            data[i][0] = selectedQueries.get(i);
        }
        return data;
    }
}
