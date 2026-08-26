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

    /**
     * Loads document-scoped query sets from src/test/resources/<fileName>
     * (for example: document_queries.json).
     */
    public static List<DocumentQuerySet> getDocumentQueries(String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = QueryData.class
                    .getClassLoader()
                    .getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new RuntimeException("Could not find " + fileName + " in src/test/resources");
            }

            return mapper.readValue(inputStream, new TypeReference<List<DocumentQuerySet>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + fileName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns the query list for the given document title from document_queries.json-style data.
     */
    public static List<QueryModel> getQueriesForDocument(String fileName, String documentTitle) {
        List<DocumentQuerySet> all = getDocumentQueries(fileName);
        for (DocumentQuerySet set : all) {
            if (set.getDocument() != null && set.getDocument().equals(documentTitle)) {
                List<QueryModel> queries = set.getQueries();
                if (queries == null || queries.isEmpty()) {
                    throw new RuntimeException(
                            "No queries found for document: " + documentTitle + " in " + fileName
                    );
                }
                return queries;
            }
        }
        throw new RuntimeException(
                "No document entry found for title '" + documentTitle + "' in " + fileName
        );
    }
}
