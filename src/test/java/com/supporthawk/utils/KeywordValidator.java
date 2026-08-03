package com.supporthawk.utils;

import java.util.ArrayList;
import java.util.List;

public class KeywordValidator {

    public static List<String> findMatchedKeywords(String response, List<String> expectedKeywords) {

        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : expectedKeywords) {
            if (response.toLowerCase().contains(keyword.toLowerCase())) {
                matchedKeywords.add(keyword);
            }
        }

        return matchedKeywords;
    }
}