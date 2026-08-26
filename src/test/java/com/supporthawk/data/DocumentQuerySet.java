package com.supporthawk.data;

import java.util.List;

/**
 * Maps one entry in document_queries.json: a document title and its related queries.
 */
public class DocumentQuerySet {

    private String document;
    private List<QueryModel> queries;

    public DocumentQuerySet() {
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public List<QueryModel> getQueries() {
        return queries;
    }

    public void setQueries(List<QueryModel> queries) {
        this.queries = queries;
    }
}
