package com.supporthawk.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared {@code -DtestGroups} filtering for query JSON entries.
 * Used by Fintech DataProviders and Josh document runners so suite selection
 * stays consistent: keep rows whose {@code tags} list contains the requested
 * group (case-insensitive). When the property is absent/blank, keep everything.
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
     * {@code true} when a non-blank {@code testGroups} filter is active.
     */
    public static boolean isFilterActive() {
        String testGroups = requestedTestGroups();
        return testGroups != null && !testGroups.trim().isEmpty();
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
