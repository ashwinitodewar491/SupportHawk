package com.supporthawk.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.supporthawk.config.AppConfig;
import com.supporthawk.data.QueryModel;
import com.supporthawk.report.QueryReportRecorder;
import com.supporthawk.report.QueryResult;
import com.supporthawk.report.QueryResultCollector;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Extent report listener plus observational query-result collection for the
 * HTML dashboard. Does not alter assertion behavior.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    @Override
    public void onStart(ISuite suite) {
        QueryResultCollector.suiteStarted();

        String env = System.getProperty("env") != null ? System.getProperty("env") : "local";
        String group = System.getProperty("testGroups") != null ? System.getProperty("testGroups") : "all";

        ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-report/index.html");
        spark.config().setDocumentTitle("SupportHawk Test Report");
        spark.config().setReportName("Report — " + env.toUpperCase() + " | " + group.toUpperCase());
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setTimelineEnabled(true);

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", env);
        extent.setSystemInfo("Suite / Group", group);
        extent.setSystemInfo("Base URL", AppConfig.BASE_URL);
        extent.setSystemInfo("Headless", String.valueOf(AppConfig.isHeadless()));
        extent.setSystemInfo("Java", System.getProperty("java.version"));
    }

    @Override
    public void onFinish(ISuite suite) {
        QueryResultCollector.suiteFinished();
        if (extent != null) {
            extent.flush();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        QueryResultCollector.markStart();

        if (extent == null) {
            return;
        }

        String description = result.getMethod().getDescription();
        String name = (description != null && !description.isBlank())
                ? description
                : result.getMethod().getMethodName();

        QueryModel model = extractQueryModel(result);
        if (model != null && model.getQuery() != null && !model.getQuery().isBlank()) {
            name = model.getQuery();
        }

        ExtentTest test = extent.createTest(name);
        test.assignCategory(result.getMethod().getGroups());
        extentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        recordQueryResult(result, QueryResult.Status.PASSED);
        if (extentTest.get() != null) {
            extentTest.get().pass("PASSED");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recordQueryResult(result, QueryResult.Status.FAILED);
        if (extentTest.get() != null) {
            extentTest.get().fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        recordQueryResult(result, QueryResult.Status.SKIPPED);
        if (extentTest.get() != null) {
            Throwable t = result.getThrowable();
            extentTest.get().skip(t != null ? t : new Exception("Skipped"));
        }
    }

    /**
     * Admin soft-collects per-query results itself. Skip the aggregated
     * TestNG failure/success so the dashboard is not polluted with a synthetic row.
     */
    private static boolean isAdminAggregatedMethod(ITestResult result) {
        String className = result.getTestClass().getRealClass().getName();
        String method = result.getMethod().getMethodName();
        return className.contains("AdminTest") && "testAdminDocumentUpload".equals(method);
    }

    private static void recordQueryResult(ITestResult result, QueryResult.Status status) {
        if (isAdminAggregatedMethod(result)) {
            QueryReportRecorder.clear();
            return;
        }

        QueryModel model = extractQueryModel(result);
        String className = result.getTestClass().getRealClass().getName();
        String flow = QueryResultCollector.classifyFlow(className);

        QueryResult qr = QueryReportRecorder.take();
        if (qr == null) {
            qr = new QueryResult();
            qr.setFlow(flow);
            qr.setQuery(resolveQueryText(result, model));
            if (model != null && model.getExpected() != null) {
                qr.setExpectedResponse(String.valueOf(model.getExpected()));
            }
            if ("Customer Post-Login".equals(flow) && model != null) {
                qr.setIntent(blankToNull(model.getIntent()));
                qr.setLanguage(blankToNull(model.getLanguage()));
            } else {
                qr.setIntent(null);
                if (model != null) {
                    qr.setLanguage(blankToNull(model.getLanguage()));
                }
            }
        }

        qr.setTestClass(result.getTestClass().getRealClass().getSimpleName());
        qr.setTestMethod(result.getMethod().getMethodName());
        if (qr.getFlow() == null || qr.getFlow().isBlank()) {
            qr.setFlow(flow);
        }
        if (qr.getQuery() == null || qr.getQuery().isBlank()) {
            qr.setQuery(resolveQueryText(result, model));
        }
        if (qr.getStartTime() == null || qr.getStartTime().isBlank()) {
            qr.setStartTime(QueryResultCollector.formatEpoch(result.getStartMillis()));
        }
        qr.setEndTime(QueryResultCollector.formatEpoch(
                result.getEndMillis() > 0 ? result.getEndMillis() : System.currentTimeMillis()
        ));
        long duration = result.getEndMillis() > 0
                ? Math.max(0L, result.getEndMillis() - result.getStartMillis())
                : QueryResultCollector.takeDurationMs();
        qr.setDurationMs(duration);

        if (status == QueryResult.Status.SKIPPED) {
            qr.setOverallStatus(QueryResult.Status.SKIPPED.name());
        } else if (hasValidationStatuses(qr)) {
            qr.refreshOverallStatus();
            if (status == QueryResult.Status.FAILED
                    && !QueryResult.Status.FAILED.name().equalsIgnoreCase(qr.getOverallStatus())) {
                qr.setOverallStatus(QueryResult.Status.FAILED.name());
            }
        } else {
            qr.setOverallStatus(status.name());
        }

        if (status == QueryResult.Status.FAILED && result.getThrowable() != null) {
            Throwable t = result.getThrowable();
            if (qr.getErrorMessage() == null || qr.getErrorMessage().isBlank()) {
                qr.setErrorMessage(t.getMessage());
            }
            // Never overwrite a captured bot response with the throwable message.
            qr.setStackTrace(stackTraceOf(t));
        }

        QueryResultCollector.add(qr);
    }

    private static boolean hasValidationStatuses(QueryResult qr) {
        return (qr.getResponseValidationStatus() != null && !qr.getResponseValidationStatus().isBlank())
                || (qr.getDocumentValidationStatus() != null && !qr.getDocumentValidationStatus().isBlank());
    }

    private static String resolveQueryText(ITestResult result, QueryModel model) {
        if (model != null && model.getQuery() != null && !model.getQuery().isBlank()) {
            return model.getQuery();
        }
        if (result.getParameters() != null) {
            for (Object param : result.getParameters()) {
                if (param instanceof QueryModel qm && qm.getQuery() != null && !qm.getQuery().isBlank()) {
                    return qm.getQuery();
                }
                if (param instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        }
        String name = result.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return result.getMethod().getMethodName();
    }

    private static QueryModel extractQueryModel(ITestResult result) {
        if (result.getParameters() == null) {
            return null;
        }
        for (Object param : result.getParameters()) {
            if (param instanceof QueryModel) {
                return (QueryModel) param;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
