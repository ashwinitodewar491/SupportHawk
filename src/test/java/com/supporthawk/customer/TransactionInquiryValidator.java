package com.supporthawk.customer;

import com.supporthawk.data.QueryModel;
import com.supporthawk.pages.QueryPage;
import org.testng.Assert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates transaction_inquiry bot responses using the structured HTML table
 * extracted by QueryPage (headers + tbody rows), not rendered text.
 *
 * Confirmed cell order: Date, Description, Category, Type, Amount.
 */
final class TransactionInquiryValidator {

    private static final int DATE_COLUMN = 0;
    private static final int DESCRIPTION_COLUMN = 1;

    private static final Pattern RECENT_N_PATTERN =
            Pattern.compile("(?i)\\b(\\d+)\\s+of\\s+my\\s+recent\\s+transactions\\b");

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private TransactionInquiryValidator() {
    }

    static void validate(QueryPage queryPage, QueryModel queryModel) {
        List<String> headers = queryPage.getLatestResponseTableHeaders();
        List<List<String>> rows = queryPage.getLatestResponseTableRows();

        Assert.assertFalse(
                rows.isEmpty(),
                "Expected a transaction table in bot response but found no transaction rows."
        );

        String q = queryModel.getQuery() == null ? "" : queryModel.getQuery().toLowerCase();
        String query = queryModel.getQuery();
        int dateIndex = resolveColumnIndex(headers, "date", DATE_COLUMN);
        int descriptionIndex = resolveColumnIndex(headers, "description", DESCRIPTION_COLUMN);

        if (isUpiQuery(q)) {
            Assert.assertTrue(
                    rows.size() >= 1,
                    "UPI query must return at least 1 transaction. Found " + rows.size()
                            + " for query: " + query
            );
            assertUpiInDescription(rows, descriptionIndex, query);
            return;
        }

        if (isLatestTransactionQuery(q)) {
            assertRowCount(rows, 1, query);
            assertSortedByLatestDateFirst(rows, dateIndex, query);
            return;
        }

        Integer requestedRecentCount = extractRecentCount(q);
        if (requestedRecentCount != null) {
            assertRowCount(rows, requestedRecentCount, query);
            assertSortedByLatestDateFirst(rows, dateIndex, query);
            return;
        }

        if (isRecentFiveQuery(q)) {
            assertRowCount(rows, 5, query);
            assertSortedByLatestDateFirst(rows, dateIndex, query);
        }
    }

    private static boolean isUpiQuery(String q) {
        return q.contains("upi");
    }

    private static boolean isLatestTransactionQuery(String q) {
        return q.contains("latest transaction");
    }

    private static boolean isRecentFiveQuery(String q) {
        return q.contains("recent transactions")
                || q.contains("transactions from my savings account");
    }

    private static Integer extractRecentCount(String q) {
        Matcher matcher = RECENT_N_PATTERN.matcher(q);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static int resolveColumnIndex(List<String> headers, String columnName, int fallbackIndex) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i) == null ? "" : headers.get(i).toLowerCase();
            if (header.contains(columnName)) {
                return i;
            }
        }
        return fallbackIndex;
    }

    private static void assertRowCount(List<List<String>> rows, int expected, String query) {
        Assert.assertEquals(
                rows.size(),
                expected,
                "Unexpected transaction row count for query: " + query
        );
    }

    private static void assertSortedByLatestDateFirst(List<List<String>> rows, int dateIndex, String query) {
        LocalDate previous = null;
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            Assert.assertTrue(
                    dateIndex < row.size(),
                    "Date column missing in row " + (i + 1) + " for query: " + query
            );
            LocalDate current = parseDate(row.get(dateIndex), query, i + 1);
            if (previous != null) {
                Assert.assertFalse(
                        current.isAfter(previous),
                        "Transactions are not sorted latest-first for query: " + query
                );
            }
            previous = current;
        }
    }

    private static void assertUpiInDescription(List<List<String>> rows, int descriptionIndex, String query) {
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            Assert.assertTrue(
                    descriptionIndex < row.size(),
                    "Description column missing in row " + (i + 1) + " for query: " + query
            );
            String description = row.get(descriptionIndex) == null
                    ? ""
                    : row.get(descriptionIndex).toLowerCase();
            Assert.assertTrue(
                    description.contains("upi"),
                    "Description does not contain UPI for row " + (i + 1) + " in query: " + query
            );
        }
    }

    private static LocalDate parseDate(String rawDate, String query, int rowNumber) {
        String value = rawDate == null ? "" : rawDate.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format
            }
        }
        throw new AssertionError(
                "Unsupported transaction date format '" + value + "' in row " + rowNumber
                        + " for query: " + query
        );
    }
}
