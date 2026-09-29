package com.supporthawk.utils;

import com.microsoft.playwright.Page;
import com.supporthawk.report.QueryReportRecorder;
import com.supporthawk.report.QueryResult;
import com.supporthawk.report.QueryResultCollector;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

    private static final Path FAILED_DIR = Paths.get("target", "screenshots", "failed");
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {
    }

    /**
     * Captures a screenshot to {@code target/screenshots/failed/} when {@code result}
     * is a failure. Returns the path, or null when nothing was captured.
     * No-op on success, a null/closed page, or if the capture itself throws.
     */
    public static String captureOnFailure(Page page, ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE || page == null || page.isClosed()) {
            return null;
        }
        String label = result.getParameters().length > 0
                ? result.getParameters()[0].toString()
                : result.getMethod().getMethodName();
        String path = captureRaw(page, label);
        if (path != null) {
            QueryResultCollector.attachScreenshotToLast(path);
        }
        return path;
    }

    /**
     * Captures a failure screenshot while the browser is still open.
     * Associates the path with {@link QueryReportRecorder#current()} when present;
     * otherwise returns the path for the caller to attach to its {@link QueryResult}.
     */
    public static String captureForLabel(Page page, String label) {
        String path = captureRaw(page, label);
        if (path == null) {
            return null;
        }
        QueryResult current = QueryReportRecorder.current();
        if (current != null
                && (current.getScreenshotPath() == null || current.getScreenshotPath().isBlank())) {
            current.setScreenshotPath(path);
        }
        return path;
    }

    private static String captureRaw(Page page, String label) {
        if (page == null || page.isClosed()) {
            return null;
        }
        try {
            Files.createDirectories(FAILED_DIR);
            String safeLabel = sanitize(label);
            if (safeLabel.isBlank()) {
                safeLabel = "failure";
            }
            if (safeLabel.length() > 80) {
                safeLabel = safeLabel.substring(0, 80);
            }
            String fileName = safeLabel + "_" + FILE_TS.format(LocalDateTime.now()) + ".png";
            Path screenshotPath = FAILED_DIR.resolve(fileName);
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            String path = screenshotPath.toString().replace('\\', '/');
            System.out.println("[Screenshot] " + path);
            return path;
        } catch (Exception e) {
            System.out.println("[Screenshot] Failed to capture: " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
