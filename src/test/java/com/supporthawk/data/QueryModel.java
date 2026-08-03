package com.supporthawk.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple model (POJO) that maps to one object inside fintech_queries.json.
 * Each object has a query, expected keywords, and one or more suite tags.
 */
public class QueryModel {

    /** The question text sent to the Query page. */
    private String query;

    /**
     * Keywords that should appear in the AI response.
     * Can be one keyword, e.g. ["interest"], or several, e.g. ["passport", "pan"].
     */
    private List<String> expected;

    /**
     * Suite tags for this query, e.g. ["smoke"] or ["smoke", "regression"].
     * A query can belong to more than one suite.
     */
    private List<String> tags;

    /** Empty constructor needed by Jackson when reading JSON. */
    public QueryModel() {
    }

    /** Convenience constructor for creating a QueryModel in code. */
    public QueryModel(String query, List<String> expected) {
        this.query = query;
        this.expected = expected;
    }

    /** Returns the question text. */
    public String getQuery() {
        return query;
    }

    /** Sets the question text. */
    public void setQuery(String query) {
        this.query = query;
    }

    /** Returns the list of expected keywords. */
    public List<String> getExpected() {
        return expected;
    }

    /** Sets the list of expected keywords. */
    public void setExpected(List<String> expected) {
        this.expected = expected;
    }

    /** Returns the suite tags (may be null if none were set). */
    public List<String> getTags() {
        return tags;
    }

    /** Sets the suite tags from a JSON array, e.g. "tags": ["smoke", "regression"]. */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Legacy support: older JSON used a single field "group": "smoke".
     * Jackson calls this setter, and we store the value inside tags.
     */
    public void setGroup(String group) {
        addTag(group);
    }

    /**
     * Legacy support: older JSON used a single field "tag": "smoke".
     * Jackson calls this setter, and we store the value inside tags.
     */
    public void setTag(String tag) {
        addTag(tag);
    }

    /**
     * Adds one tag to the tags list if it is not already present.
     * Used by the legacy setGroup / setTag methods.
     */
    private void addTag(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        // Skip if this tag is already in the list (case-insensitive)
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i) != null && tags.get(i).equalsIgnoreCase(value)) {
                return;
            }
        }
        tags.add(value);
    }
}
