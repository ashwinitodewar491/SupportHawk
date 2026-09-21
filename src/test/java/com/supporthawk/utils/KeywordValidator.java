package com.supporthawk.utils;

import java.util.ArrayList;
import java.util.List;

public class KeywordValidator {

    public static List<String> findMatchedKeywords(String response, List<String> expectedKeywords) {

        List<String> matchedKeywords = new ArrayList<>();

        String safeResponse = response == null ? "" : response;
        String normalizedResponse = safeResponse
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        for (String keyword : expectedKeywords) {
            String normalizedKeyword = keyword
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase();

            if (normalizedResponse.contains(normalizedKeyword)) {
                matchedKeywords.add(keyword);
            }
        }

        return matchedKeywords;
    }

    /**
     * Returns how many keyword matches are required based on the expected keyword count:
     * 1 keyword → 1 match, 2 → 2, 3 → 2, 4+ → 3.
     */
    public static int getRequiredMatches(int keywordCount) {
        if (keywordCount <= 0) {
            return 0;
        }
        if (keywordCount == 1) {
            return 1;
        }
        if (keywordCount == 2) {
            return 2;
        }
        if (keywordCount == 3) {
            return 2;
        }
        return 3;
    }
}
