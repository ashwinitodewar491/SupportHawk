package com.supporthawk.tests;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.utils.KeywordValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Voice-query test flow that reuses the same JSON data and validation logic
 * as the existing text tests.
 */
public class VoiceQueryTest extends BasePage {

    /**
     * Runs one voice query from configured JSON and validates the response.
     * The DataProvider is reused from SupportHawkPreLoginQueryTest.
     *
     * @param queryModel one query entry from configured query file
     */
    @Test(
            dataProvider = "queryData",
            dataProviderClass = SupportHawkPreLoginQueryTest.class,
            description = "Verify voice query responses contain enough expected keywords"
    )
    public void verifyVoiceQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);

        // Open Query page and ask using microphone + Edge TTS playback.
        queryPage.navigate();
        String response = queryPage.askVoiceQuestion(queryModel.getQuery());

        // Reuse existing keyword validation approach.
        List<String> expectedKeywords = queryModel.getExpected();
        List<String> matchedKeywords = KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.findMatchedKeywords(response, expectedKeywords).size();

        int totalMatched = matchedKeywords.size();
        int totalExpected = expectedKeywords.size();

        System.out.println("--------------------------------------------------");
        System.out.println("Voice Query: " + queryModel.getQuery());
        System.out.println("Expected: " + expectedKeywords);
        System.out.println("Matched: " + matchedKeywords);
        System.out.println("Match count: " + totalMatched + "/" + totalExpected);
        System.out.println("Response: " + response);
        System.out.println("--------------------------------------------------");

        Assert.assertTrue(
                totalMatched >= requiredMatches,
                "Not enough keywords matched for voice query. Needed at least " + requiredMatches
                        + " but found " + totalMatched
                        + ". Expected: " + expectedKeywords
                        + ". Matched: " + matchedKeywords
                        + ". Response: " + response
        );
    }
}
