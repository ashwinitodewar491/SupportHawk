package com.supporthawk.data;

import java.util.List;

/**
 * Maps one entry in document_queries.json: document metadata plus its queries.
 */
public class DocumentQuerySet {

    private String document;
    private String pdf;
    private List<String> tags;
    private String description;
    private List<QueryModel> queries;

    public DocumentQuerySet() {
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getPdf() {
        return pdf;
    }

    public void setPdf(String pdf) {
        this.pdf = pdf;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<QueryModel> getQueries() {
        return queries;
    }

    public void setQueries(List<QueryModel> queries) {
        this.queries = queries;
    }
}
