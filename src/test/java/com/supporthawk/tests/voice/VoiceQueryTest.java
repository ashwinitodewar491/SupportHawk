package com.supporthawk.tests.voice;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import com.supporthawk.tests.prelogin.SupportHawkPreLoginQueryTest;
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

        queryPage.navigate();
        String response = queryPage.askVoiceQuestion(queryModel.getQuery());

        List<String> expectedKeywords = queryModel.getExpected();

        List<String> matchedKeywords =
                KeywordValidator.findMatchedKeywords(response, expectedKeywords);
        int requiredMatches = KeywordValidator.getRequiredMatches(expectedKeywords.size());
        int totalMatched = matchedKeywords.size();
        boolean keywordValidationPassed = totalMatched >= requiredMatches;

        System.out.println("--------------------------------------------------");
        System.out.println("BOT RESPONSE KEYWORD VALIDATION");
        System.out.println("Expected keywords: " + expectedKeywords);
        System.out.println("Required matches: " + requiredMatches);
        System.out.println("Matched keywords: " + matchedKeywords);
        System.out.println("Matched count: " + totalMatched);
        System.out.println("Keyword validation: " + (keywordValidationPassed ? "PASSED" : "FAILED"));
        System.out.println("--------------------------------------------------");

        Assert.assertTrue(
                keywordValidationPassed,
                "Not enough keywords matched in BOT RESPONSE for voice query. Needed at least "
                        + requiredMatches + " but found " + totalMatched
                        + ". Expected: " + expectedKeywords
                        + ". Matched: " + matchedKeywords
        );

        queryPage.provideRandomFeedback("This is automated test feedback for thumbs down.");

        page.waitForTimeout(10000);
        queryPage.validateReferenceLinks(expectedKeywords);

    }
}
