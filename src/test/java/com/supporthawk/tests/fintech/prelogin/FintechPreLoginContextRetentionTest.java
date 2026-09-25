package com.supporthawk.tests.fintech.prelogin;

import com.supporthawk.base.BasePage;
import com.supporthawk.context.ContextRetentionRunner;
import org.testng.annotations.Test;

/**
 * Fintech pre-login continuous context-retention conversation: one chat session,
 * multiple dependent follow-up questions from context_retention_queries.json.
 */
public class FintechPreLoginContextRetentionTest extends BasePage {

    @Test(description = "Home loan multi-turn context retention on Fintech /query (same session)")
    public void testHomeLoanContextRetention() {
        ContextRetentionRunner.runFintechPreLoginContextSuite(
                page,
                getClass().getSimpleName(),
                "testHomeLoanContextRetention",
                "Fintech pre-login context retention test completed with failures:\n"
        );
    }
}
