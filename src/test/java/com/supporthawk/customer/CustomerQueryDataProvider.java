package com.supporthawk.customer;

import com.supporthawk.config.ConfigReader;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared DataProvider logic for customer-login query tests.
 * Both text and voice test classes delegate here to avoid duplicating
 * the JSON loading and tag-filtering code.
 */
public final class CustomerQueryDataProvider {

    private CustomerQueryDataProvider() {
    }

    public static Object[][] loadFilteredQueries() {
        String queryFileName = ConfigReader.get("customer.query.file");
        List<QueryModel> allQueries = QueryData.getQueries(queryFileName);

        String testGroups = System.getProperty("testGroups");
        String intentFilter = System.getProperty("intentFilter");
        List<QueryModel> selected;

        if (testGroups == null || testGroups.trim().isEmpty()) {
            selected = allQueries;
        } else {
            selected = new ArrayList<>();
            for (QueryModel query : allQueries) {
                if (hasTag(query, testGroups)) {
                    selected.add(query);
                }
            }
        }

        if (intentFilter != null && !intentFilter.trim().isEmpty()) {
            List<QueryModel> intentSelected = new ArrayList<>();
            for (QueryModel query : selected) {
                String intent = query.getIntent();
                if (intent != null && intent.equalsIgnoreCase(intentFilter.trim())) {
                    intentSelected.add(query);
                }
            }
            selected = intentSelected;
        }

        Object[][] data = new Object[selected.size()][1];
        for (int i = 0; i < selected.size(); i++) {
            data[i][0] = selected.get(i);
        }
        return data;
    }

    private static boolean hasTag(QueryModel query, String suiteName) {
        List<String> tags = query.getTags();
        if (tags == null) {
            return false;
        }
        for (String tag : tags) {
            if (tag != null && tag.equalsIgnoreCase(suiteName)) {
                return true;
            }
        }
        return false;
    }
}
