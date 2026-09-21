package com.supporthawk.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory + on-disk collector of per-query results for the HTML dashboard.
 * Persists to {@code target/dashboard-report/query-results.jsonl} so a separate
 * Maven JVM can generate the report after tests finish.
 */
public final class QueryResultCollector {

    public static final Path RESULTS_FILE =
            Paths.get("target", "dashboard-report", "query-results.jsonl");
    public static final Path META_FILE =
            Paths.get("target", "dashboard-report", "run-meta.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final List<QueryResult> RESULTS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Long> START_MS = new ThreadLocal<>();
    private static final ThreadLocal<QueryResult> LAST_FOR_THREAD = new ThreadLocal<>();

    private static volatile long suiteStartEpochMs;
    private static volatile long suiteEndEpochMs;

    private QueryResultCollector() {
    }

    public static void suiteStarted() {
        RESULTS.clear();
        suiteStartEpochMs = System.currentTimeMillis();
        suiteEndEpochMs = 0L;
        try {
            Files.createDirectories(RESULTS_FILE.getParent());
            Files.writeString(RESULTS_FILE, "", StandardCharsets.UTF_8);
            writeMeta();
        } catch (Exception e) {
            System.out.println("[Report] Failed to reset result store: " + e.getMessage());
        }
    }

    public static void suiteFinished() {
        suiteEndEpochMs = System.currentTimeMillis();
        writeMeta();
    }

    public static void markStart() {
        START_MS.set(System.currentTimeMillis());
    }

    public static long takeDurationMs() {
        Long start = START_MS.get();
        START_MS.remove();
        if (start == null) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - start);
    }

    public static String formatNow() {
        return TIME_FMT.format(Instant.now());
    }

    public static String formatEpoch(long epochMs) {
        if (epochMs <= 0L) {
            return "-";
        }
        return TIME_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    public static void add(QueryResult result) {
        if (result == null) {
            return;
        }
        RESULTS.add(result);
        LAST_FOR_THREAD.set(result);
        appendJsonLine(result);
    }

    /**
     * Attaches a screenshot path to the most recent result for this thread
     * (typically the failing TestNG invocation).
     */
    public static void attachScreenshotToLast(String screenshotPath) {
        if (screenshotPath == null || screenshotPath.isBlank()) {
            return;
        }
        QueryResult last = LAST_FOR_THREAD.get();
        if (last != null) {
            last.setScreenshotPath(screenshotPath);
            rewriteAll();
        }
    }

    public static List<QueryResult> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(RESULTS));
    }

    public static List<QueryResult> loadPersisted() {
        List<QueryResult> loaded = new ArrayList<>();
        if (!Files.isRegularFile(RESULTS_FILE)) {
            return loaded;
        }
        try {
            List<String> lines = Files.readAllLines(RESULTS_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                if (line.isBlank()) {
                    continue;
                }
                loaded.add(MAPPER.readValue(line, QueryResult.class));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + RESULTS_FILE + ": " + e.getMessage(), e);
        }
        return loaded;
    }

    public static RunMeta loadMeta() {
        if (!Files.isRegularFile(META_FILE)) {
            return new RunMeta(0L, 0L);
        }
        try {
            return MAPPER.readValue(META_FILE.toFile(), RunMeta.class);
        } catch (Exception e) {
            return new RunMeta(0L, 0L);
        }
    }

    public static String classifyFlow(String className) {
        if (className == null) {
            return "Unknown";
        }
        String lower = className.toLowerCase();
        if (lower.contains(".postlogin.") || lower.contains("customerlogin")) {
            return "Customer Post-Login";
        }
        if (lower.contains(".admin.") || lower.contains("admintest")) {
            return "Admin";
        }
        if (lower.contains(".voice.") || lower.contains("voicequery")) {
            return "Voice";
        }
        if (lower.contains(".prelogin.") || lower.contains("prelogin")) {
            return "Pre-login";
        }
        return "Unknown";
    }

    private static void appendJsonLine(QueryResult result) {
        try {
            Files.createDirectories(RESULTS_FILE.getParent());
            String json = MAPPER.writeValueAsString(result);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    RESULTS_FILE,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(json);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("[Report] Failed to persist query result: " + e.getMessage());
        }
    }

    private static synchronized void rewriteAll() {
        try {
            Files.createDirectories(RESULTS_FILE.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    RESULTS_FILE,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                for (QueryResult result : RESULTS) {
                    writer.write(MAPPER.writeValueAsString(result));
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            System.out.println("[Report] Failed to rewrite query results: " + e.getMessage());
        }
    }

    private static void writeMeta() {
        try {
            Files.createDirectories(META_FILE.getParent());
            MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(META_FILE.toFile(), new RunMeta(suiteStartEpochMs, suiteEndEpochMs));
        } catch (Exception e) {
            System.out.println("[Report] Failed to write run meta: " + e.getMessage());
        }
    }

    public static final class RunMeta {
        public long suiteStartEpochMs;
        public long suiteEndEpochMs;

        public RunMeta() {
        }

        public RunMeta(long suiteStartEpochMs, long suiteEndEpochMs) {
            this.suiteStartEpochMs = suiteStartEpochMs;
            this.suiteEndEpochMs = suiteEndEpochMs;
        }
    }
}
