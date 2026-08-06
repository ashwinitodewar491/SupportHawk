package com.supporthawk.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * Helper class that reads a query JSON file from the test resources folder
 * and turns it into a list of QueryModel objects using Jackson.
 */
public class QueryData {

    /**
     * Loads all queries from src/test/resources/<fileName>.
     *
     * @param fileName JSON file name in src/test/resources (for example: fintech_queries.json)
     * @return a list of QueryModel objects (one per JSON entry)
     */
    public static List<QueryModel> getQueries(String fileName) {
        try {
            // ObjectMapper is Jackson's main class for reading/writing JSON
            ObjectMapper mapper = new ObjectMapper();

            // Open the provided JSON file from the classpath (src/test/resources)
            InputStream inputStream = QueryData.class
                    .getClassLoader()
                    .getResourceAsStream(fileName);

            // Fail clearly if the file is missing
            if (inputStream == null) {
                throw new RuntimeException("Could not find " + fileName + " in src/test/resources");
            }

            // Convert the JSON array into a List<QueryModel>
            return mapper.readValue(inputStream, new TypeReference<List<QueryModel>>() {
            });
        } catch (Exception e) {
            // Wrap any read/parse error so the test fails with a clear message
            throw new RuntimeException("Failed to read " + fileName + ": " + e.getMessage(), e);
        }
    }
}
