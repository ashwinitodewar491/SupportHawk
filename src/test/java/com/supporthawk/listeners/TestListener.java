package com.supporthawk.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.supporthawk.config.AppConfig;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Wires TestNG results into a single self-contained Extent HTML report at
 * target/extent-report/index.html. Do not modify for individual test needs. */
public class TestListener implements ITestListener, ISuiteListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    @Override
    public void onStart(ISuite suite) {
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
        if (extent != null) {
            extent.flush();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (extent == null) {
            return;
        }

        String description = result.getMethod().getDescription();
        String name = (description != null && !description.isBlank())
                ? description
                : result.getMethod().getMethodName();

        ExtentTest test = extent.createTest(name);
        test.assignCategory(result.getMethod().getGroups());
        extentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (extentTest.get() != null) {
            extentTest.get().pass("PASSED");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (extentTest.get() != null) {
            extentTest.get().fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (extentTest.get() != null) {
            Throwable t = result.getThrowable();
            extentTest.get().skip(t != null ? t : new Exception("Skipped"));
        }
    }
}
