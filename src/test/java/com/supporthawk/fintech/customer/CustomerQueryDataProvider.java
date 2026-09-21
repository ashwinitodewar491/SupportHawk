package com.supporthawk.fintech.customer;

import com.supporthawk.config.ConfigReader;
import com.supporthawk.data.QueryData;
import com.supporthawk.data.QueryModel;
import com.supporthawk.data.QueryTagFilter;

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
        List<QueryModel> selected = QueryTagFilter.filterQueries(
                QueryData.getQueries(queryFileName)
        );

        String intentFilter = System.getProperty("intentFilter");
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
}
