package com.supporthawk.report;

import com.supporthawk.data.QueryModel;

/**
 * Thread-local builder for DataProvider-driven query reporting.
 * Test code populates fields while the real response is in hand; TestListener
 * finalizes timing/error without overwriting {@code actualResponse}.
 */
public final class QueryReportRecorder {

    private static final ThreadLocal<QueryResult> CURRENT = new ThreadLocal<>();

    private QueryReportRecorder() {
    }

    public static QueryResult begin(
            String flow,
            String testClass,
            String testMethod,
            QueryModel model
    ) {
        QueryResult qr = new QueryResult();
        qr.setFlow(flow);
        qr.setTestClass(testClass);
        qr.setTestMethod(testMethod);
        qr.setStartTime(QueryResultCollector.formatNow());
        if (model != null) {
            qr.setQuery(model.getQuery());
            if (model.getExpected() != null) {
                qr.setExpectedResponse(String.valueOf(model.getExpected()));
            }
            if ("Customer Post-Login".equals(flow)) {
                qr.setIntent(blankToNull(model.getIntent()));
                qr.setLanguage(blankToNull(model.getLanguage()));
            } else {
                qr.setIntent(null);
                qr.setLanguage(blankToNull(model.getLanguage()));
            }
        }
        CURRENT.set(qr);
        return qr;
    }

    public static QueryResult current() {
        return CURRENT.get();
    }

    public static QueryResult take() {
        QueryResult qr = CURRENT.get();
        CURRENT.remove();
        return qr;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
