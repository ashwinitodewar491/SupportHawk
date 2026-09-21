package com.supporthawk.tests.fintech.prelogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.data.QueryModel;
import com.supporthawk.fintech.prelogin.FintechPreLoginQueryRunner;
import com.supporthawk.fintech.prelogin.FintechPreLoginQueryDataProvider;
import com.supporthawk.pages.QueryPage;
import org.testng.annotations.Test;

/**
 * Voice-query test flow that reuses the same JSON data and validation logic
 * as the existing text tests.
 */
public class FintechPreLoginVoiceTest extends BasePage {

    /**
     * Runs one voice query from configured JSON and validates the response.
     * The DataProvider is reused from FintechPreLoginQueryDataProvider.
     *
     * @param queryModel one query entry from configured query file
     */
    @Test(
            dataProvider = "queryData",
            dataProviderClass = FintechPreLoginQueryDataProvider.class,
            description = "Verify voice query responses contain enough expected keywords"
    )
    public void verifyVoiceQueryResponse(QueryModel queryModel) {
        QueryPage queryPage = new QueryPage(page);
        FintechPreLoginQueryRunner.executeVoiceQuery(
                queryPage,
                queryModel,
                getClass().getSimpleName(),
                "verifyVoiceQueryResponse"
        );
    }
}
