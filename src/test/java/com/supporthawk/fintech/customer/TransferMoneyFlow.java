package com.supporthawk.fintech.customer;

import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal multi-step transfer flow handler for transfer_money intent.
 * It walks the conversation through beneficiary, amount, and confirmation
 * prompts until a final transfer result is reached (or max steps are exceeded).
 */
final class TransferMoneyFlow {

    private static final String DEFAULT_BENEFICIARY = "Pooja Nair";
    private static final String DEFAULT_AMOUNT = "1 rupee";
    private static final int MAX_STEPS = 6;

    private static final Pattern RUPEE_AMOUNT_PATTERN =
            Pattern.compile("(?i)\\b(\\d+)\\s*(rupee|rupees|rs|inr)\\b");

    private TransferMoneyFlow() {
    }

    static String run(QueryPage queryPage, QueryModel queryModel, boolean voice) {
        String initialQuery = queryModel.getQuery();
        TransferDetails details = resolveDetails(initialQuery, queryModel);

        boolean beneficiarySent = details.queryHasBeneficiary;
        boolean amountSent = details.queryHasAmount;

        String response = send(queryPage, initialQuery, voice);

        for (int step = 0; step < MAX_STEPS; step++) {
            if (asksForConfirmationOrCancel(response)) {
                // Current SupportHawk flow: confirm/cancel authorization prompt is terminal.
                return response;
            }
            if (isFinalTransferResult(response)) {
                return response;
            }

            String followUp = null;
            if (!beneficiarySent && asksForBeneficiary(response)) {
                followUp = details.beneficiary;
                beneficiarySent = true;
            } else if (!amountSent && asksForAmount(response)) {
                followUp = details.amount;
                amountSent = true;
            }

            if (followUp == null) {
                break;
            }

            String previous = response;
            response = send(queryPage, followUp, voice);
            assertConversationProgressed(previous, response, followUp);
        }

        return response;
    }

    private static String send(QueryPage queryPage, String text, boolean voice) {
        return DisambiguationResolver.sendFollowUp(queryPage, text, voice);
    }

    private static TransferDetails resolveDetails(String query, QueryModel queryModel) {
        String normalized = query == null ? "" : query.toLowerCase();
        boolean hasBeneficiary = normalized.contains("to ") || normalized.contains("beneficiary");
        boolean hasAmount = RUPEE_AMOUNT_PATTERN.matcher(normalized).find();

        String configuredBeneficiary = queryModel.getDisambiguationResponse();
        String beneficiary = extractBeneficiary(query);
        if (beneficiary == null || beneficiary.isBlank()) {
            beneficiary = configuredBeneficiary;
        }
        if (beneficiary == null || beneficiary.isBlank()) {
            beneficiary = DEFAULT_BENEFICIARY;
        }

        String amount = extractAmount(query);
        if (amount == null || amount.isBlank()) {
            amount = DEFAULT_AMOUNT;
        }

        return new TransferDetails(beneficiary, amount, hasBeneficiary, hasAmount);
    }

    private static String extractBeneficiary(String query) {
        if (query == null) {
            return null;
        }
        String lower = query.toLowerCase();
        int toIndex = lower.indexOf(" to ");
        if (toIndex >= 0) {
            String tail = query.substring(toIndex + 4).trim();
            int rupeeIndex = tail.toLowerCase().indexOf("rupee");
            if (rupeeIndex > 0) {
                tail = tail.substring(0, rupeeIndex).trim();
            }
            if (!tail.isBlank()) {
                return tail;
            }
        }
        if (lower.contains("pooja nair")) {
            return "Pooja Nair";
        }
        return null;
    }

    private static String extractAmount(String query) {
        if (query == null) {
            return null;
        }
        Matcher matcher = RUPEE_AMOUNT_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + " rupee";
        }
        if (query.toLowerCase().contains("one rupee")) {
            return "1 rupee";
        }
        return null;
    }

    private static void assertConversationProgressed(String previousResponse, String nextResponse, String followUp) {
        String prev = normalize(previousResponse);
        String next = normalize(nextResponse);
        Assert.assertFalse(
                next.isBlank(),
                "Transfer flow follow-up produced an empty bot response. Follow-up: " + followUp
        );
        Assert.assertNotEquals(
                next,
                prev,
                "Transfer flow did not progress after follow-up: " + followUp
        );
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static boolean asksForBeneficiary(String response) {
        String lower = normalize(response);
        return lower.contains("beneficiary")
                || lower.contains("available options")
                || lower.contains("who would you like to transfer");
    }

    private static boolean asksForAmount(String response) {
        String lower = normalize(response);
        return lower.contains("how much")
                || lower.contains("amount")
                || lower.contains("how many rupees");
    }

    private static boolean asksForConfirmationOrCancel(String response) {
        String lower = normalize(response);
        return (lower.contains("confirm") && lower.contains("cancel"))
                || lower.contains("to authorize this transfer")
                || lower.contains("please cancel my money transfer")
                || lower.contains("should i proceed")
                || lower.contains("do you want to continue");
    }

    private static boolean isFinalTransferResult(String response) {
        String lower = normalize(response);
        return (lower.contains("transfer") && (lower.contains("success") || lower.contains("successful")))
                || lower.contains("transaction completed")
                || lower.contains("money has been transferred");
    }

    private record TransferDetails(String beneficiary, String amount, boolean queryHasBeneficiary, boolean queryHasAmount) {
    }
}
