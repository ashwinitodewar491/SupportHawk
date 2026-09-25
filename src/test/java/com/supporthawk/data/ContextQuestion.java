package com.supporthawk.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One turn inside a multi-question context-retention conversation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextQuestion {

    private String id;
    private String query;
    private List<String> expectedKeywords;
    /** Metadata only — same-session ordering preserves context. */
    private boolean contextDependency;

    public ContextQuestion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getExpectedKeywords() {
        return expectedKeywords;
    }

    public void setExpectedKeywords(List<String> expectedKeywords) {
        this.expectedKeywords = expectedKeywords;
    }

    public boolean isContextDependency() {
        return contextDependency;
    }

    public void setContextDependency(boolean contextDependency) {
        this.contextDependency = contextDependency;
    }
}
