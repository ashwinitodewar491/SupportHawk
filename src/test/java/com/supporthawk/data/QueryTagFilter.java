package com.supporthawk.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared {@code -DtestGroups} / {@code -Dlanguages} filtering for query JSON entries.
 * Used by Fintech DataProviders and Josh document runners so suite selection
 * stays consistent. When a property is absent/blank, that filter is a no-op.
 */
public final class QueryTagFilter {

    private QueryTagFilter() {
    }

    /**
     * Returns the raw Maven {@code -DtestGroups} value, or {@code null}.
     */
    public static String requestedTestGroups() {
        return System.getProperty("testGroups");
    }

    /**
     * Returns the raw Maven {@code -Dlanguages} value, or {@code null}.
     * Example: {@code -Dlanguages=Hindi,Bengali}
     */
    public static String requestedLanguages() {
        return System.getProperty("languages");
    }

    /**
     * {@code true} when a non-blank {@code testGroups} filter is active.
     */
    public static boolean isFilterActive() {
        String testGroups = requestedTestGroups();
        return testGroups != null && !testGroups.trim().isEmpty();
    }

    /**
     * {@code true} when a non-blank {@code languages} filter is active.
     */
    public static boolean isLanguageFilterActive() {
        String languages = requestedLanguages();
        return languages != null && !languages.trim().isEmpty();
    }

    /**
     * Returns true if {@code tags} contains {@code suiteName} (case-insensitive).
     */
    public static boolean hasTag(List<String> tags, String suiteName) {
        if (tags == null || suiteName == null) {
            return false;
        }
        for (String tag : tags) {
            if (tag != null && tag.equalsIgnoreCase(suiteName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the query's tags list contains {@code suiteName}.
     */
    public static boolean hasTag(QueryModel query, String suiteName) {
        if (query == null) {
            return false;
        }
        return hasTag(query.getTags(), suiteName);
    }

    /**
     * Filters queries by {@code System.getProperty("testGroups")}.
     * No/blank property → returns the same list (all queries).
     */
    public static List<QueryModel> filterQueries(List<QueryModel> queries) {
        if (queries == null) {
            return List.of();
        }
        if (!isFilterActive()) {
            return queries;
        }
        String suiteName = requestedTestGroups().trim();
        List<QueryModel> selected = new ArrayList<>();
        for (QueryModel query : queries) {
            if (hasTag(query, suiteName)) {
                selected.add(query);
            }
        }
        return selected;
    }

    /**
     * Filters queries by {@code System.getProperty("languages")} (comma-separated).
     * Keeps rows whose {@code language} field matches any requested value
     * (case-insensitive). No/blank property → returns the same list.
     */
    public static List<QueryModel> filterByLanguages(List<QueryModel> queries) {
        if (queries == null) {
            return List.of();
        }
        if (!isLanguageFilterActive()) {
            return queries;
        }

        Set<String> allowed = parseLanguages(requestedLanguages());
        if (allowed.isEmpty()) {
            return queries;
        }

        List<QueryModel> selected = new ArrayList<>();
        for (QueryModel query : queries) {
            if (query == null) {
                continue;
            }
            String language = query.getLanguage();
            if (language != null && allowed.contains(language.trim().toLowerCase(Locale.ROOT))) {
                selected.add(query);
            }
        }
        return selected;
    }

    /**
     * Filters each document set's nested queries by {@code testGroups}.
     * Documents with no matching queries are omitted.
     * No/blank property → returns the same list (all documents/queries).
     * Document-level tags are left unchanged (they are upload metadata, not suite tags).
     */
    public static List<DocumentQuerySet> filterDocumentSets(List<DocumentQuerySet> documents) {
        if (documents == null) {
            return List.of();
        }
        if (!isFilterActive()) {
            return documents;
        }

        List<DocumentQuerySet> selected = new ArrayList<>();
        for (DocumentQuerySet documentSet : documents) {
            if (documentSet == null) {
                continue;
            }
            List<QueryModel> filteredQueries = filterQueries(documentSet.getQueries());
            if (filteredQueries.isEmpty()) {
                continue;
            }
            selected.add(copyWithQueries(documentSet, filteredQueries));
        }
        return selected;
    }

    private static Set<String> parseLanguages(String raw) {
        Set<String> allowed = new LinkedHashSet<>();
        if (raw == null) {
            return allowed;
        }
        for (String part : raw.split(",")) {
            if (part == null) {
                continue;
            }
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                allowed.add(normalized);
            }
        }
        return allowed;
    }

    private static DocumentQuerySet copyWithQueries(
            DocumentQuerySet source,
            List<QueryModel> queries
    ) {
        DocumentQuerySet copy = new DocumentQuerySet();
        copy.setDocument(source.getDocument());
        copy.setPdf(source.getPdf());
        copy.setTags(source.getTags());
        copy.setDescription(source.getDescription());
        copy.setQueries(queries);
        return copy;
    }
}
