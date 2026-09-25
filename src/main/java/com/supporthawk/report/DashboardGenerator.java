package com.supporthawk.report;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Post-test entry point that builds {@code target/dashboard-report/index.html}
 * from persisted query results.
 */
public final class DashboardGenerator {

    private DashboardGenerator() {
    }

    public static void main(String[] args) throws Exception {
        List<QueryResult> results = QueryResultCollector.loadPersisted();
        QueryResultCollector.RunMeta meta = QueryResultCollector.loadMeta();
        if (meta.suiteEndEpochMs <= 0L) {
            meta.suiteEndEpochMs = System.currentTimeMillis();
        }

        String html = HtmlReportRenderer.render(results, meta);
        Path outDir = Paths.get("target", "dashboard-report");
        Files.createDirectories(outDir);
        Path index = outDir.resolve("index.html");
        Files.writeString(index, html, StandardCharsets.UTF_8);

        System.out.println("[Dashboard] Results loaded: " + results.size());
        System.out.println("[Dashboard] Report written to: " + index.toAbsolutePath());
    }
}
