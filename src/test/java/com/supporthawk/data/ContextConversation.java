package com.supporthawk.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One continuous multi-turn conversation from context_retention_queries.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextConversation {

    private String name;
    private List<String> tags;
    private String language;
    private String intent;
    private List<ContextQuestion> questions;

    public ContextConversation() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<ContextQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ContextQuestion> questions) {
        this.questions = questions;
    }
}
