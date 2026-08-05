package com.supporthawk.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * Helper class that reads fintech_queries.json from the test resources folder
 * and turns it into a list of QueryModel objects using Jackson.
 */
public class QueryData {

    /**
     * Loads all queries from src/test/resources/fintech_queries.json.
     *
     * @return a list of QueryModel objects (one per JSON entry)
     */
    public static List<QueryModel> getQueries() {
        try {
            // ObjectMapper is Jackson's main class for reading/writing JSON
            ObjectMapper mapper = new ObjectMapper();

            // Open fintech_queries.json from the classpath (src/test/resources)
            InputStream inputStream = QueryData.class
                    .getClassLoader()
                    .getResourceAsStream("fintech_queries.json");

            // Fail clearly if the file is missing
            if (inputStream == null) {
                throw new RuntimeException("Could not find fintech_queries.json in src/test/resources");
            }

            // Convert the JSON array into a List<QueryModel>
            return mapper.readValue(inputStream, new TypeReference<List<QueryModel>>() {
            });
        } catch (Exception e) {
            // Wrap any read/parse error so the test fails with a clear message
            throw new RuntimeException("Failed to read fintech_queries.json: " + e.getMessage(), e);
        }
    }
}
