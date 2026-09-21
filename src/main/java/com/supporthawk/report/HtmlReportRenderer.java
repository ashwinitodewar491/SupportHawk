package com.supporthawk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a self-contained SupportHawk HTML dashboard from query results.
 */
public final class HtmlReportRenderer {

    private HtmlReportRenderer() {
    }

    public static String render(List<QueryResult> results, QueryResultCollector.RunMeta meta) {
        List<QueryResult> safe = results != null ? results : List.of();
        int total = safe.size();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        long durationSum = 0L;
        for (QueryResult r : safe) {
            if (r.isPassed()) {
                passed++;
            } else if (r.isFailed()) {
                failed++;
            } else if (r.isSkipped()) {
                skipped++;
            }
            durationSum += Math.max(0L, r.getDurationMs());
        }
        double passPct = total == 0 ? 0.0 : (passed * 100.0 / total);
        long suiteDuration = 0L;
        if (meta != null && meta.suiteStartEpochMs > 0 && meta.suiteEndEpochMs >= meta.suiteStartEpochMs) {
            suiteDuration = meta.suiteEndEpochMs - meta.suiteStartEpochMs;
        } else {
            suiteDuration = durationSum;
        }
        String startTs = meta != null ? QueryResultCollector.formatEpoch(meta.suiteStartEpochMs) : "-";
        String endTs = meta != null ? QueryResultCollector.formatEpoch(meta.suiteEndEpochMs) : "-";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"UTF-8\"/>");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>");
        html.append("<title>SupportHawk Test Dashboard</title>");
        html.append("<style>");
        html.append(css());
        html.append("</style></head><body>");
        html.append("<header class=\"hero\"><div>");
        html.append("<h1>SupportHawk Test Dashboard</h1>");
        html.append("<p class=\"sub\">Query-level execution report</p>");
        html.append("</div>");
        html.append("<div class=\"hero-meta\">");
        html.append("<div>Start: <strong>").append(esc(startTs)).append("</strong></div>");
        html.append("<div>End: <strong>").append(esc(endTs)).append("</strong></div>");
        html.append("<div>Pass rate: <strong>").append(String.format(Locale.US, "%.1f%%", passPct))
                .append("</strong></div>");
        html.append("</div></header>");

        html.append("<section class=\"cards\">");
        html.append(card("Total", String.valueOf(total), "total"));
        html.append(card("Passed", String.valueOf(passed), "pass"));
        html.append(card("Failed", String.valueOf(failed), "fail"));
        html.append(card("Skipped", String.valueOf(skipped), "skip"));
        html.append(card("Pass %", String.format(Locale.US, "%.1f%%", passPct), "pct"));
        html.append(card("Duration", formatDuration(suiteDuration), "dur"));
        html.append("</section>");

        html.append("<section class=\"viz\">");
        html.append("<div class=\"donut\" style=\"--p:").append(String.format(Locale.US, "%.2f", passPct))
                .append("\"></div>");
        html.append("<div class=\"viz-legend\">");
        html.append("<div><span class=\"dot pass\"></span>Passed ").append(passed).append("</div>");
        html.append("<div><span class=\"dot fail\"></span>Failed ").append(failed).append("</div>");
        html.append("<div><span class=\"dot skip\"></span>Skipped ").append(skipped).append("</div>");
        html.append("</div></section>");

        html.append("<section class=\"panel\">");
        html.append("<h2>Failed Queries</h2>");
        List<QueryResult> failedList = new ArrayList<>();
        for (QueryResult r : safe) {
            if (r.isFailed()) {
                failedList.add(r);
            }
        }
        if (failedList.isEmpty()) {
            html.append("<p class=\"ok\">No failed queries.</p>");
        } else {
            for (int i = 0; i < failedList.size(); i++) {
                html.append(failedCard(failedList.get(i), i));
            }
        }
        html.append("</section>");

        html.append("<section class=\"panel\">");
        html.append("<h2>All Results</h2>");
        html.append("<div class=\"filters\">");
        html.append("<input id=\"search\" type=\"search\" placeholder=\"Search query text...\"/>");
        html.append("<select id=\"statusFilter\"><option value=\"\">Status: All</option>");
        html.append("<option value=\"PASSED\">Passed</option><option value=\"FAILED\">Failed</option>");
        html.append("<option value=\"SKIPPED\">Skipped</option></select>");
        html.append("<select id=\"flowFilter\"><option value=\"\">Flow: All</option>");
        html.append("<option value=\"Customer Post-Login\">Customer Post-Login</option>");
        html.append("<option value=\"Admin\">Admin</option>");
        html.append("<option value=\"Pre-login\">Pre-login</option>");
        html.append("<option value=\"Voice\">Voice</option></select>");
        html.append("<select id=\"langFilter\"><option value=\"\">Language: All</option></select>");
        html.append("<select id=\"intentFilter\"><option value=\"\">Intent: All</option></select>");
        html.append("</div>");
        html.append("<div class=\"table-wrap\"><table id=\"resultsTable\"><thead><tr>");
        html.append("<th></th><th>Status</th><th>Query</th><th>Flow</th><th>Language</th>");
        html.append("<th>Intent</th><th>Timestamp</th><th>Duration</th>");
        html.append("</tr></thead><tbody>");
        for (int i = 0; i < safe.size(); i++) {
            html.append(resultRow(safe.get(i), i));
        }
        html.append("</tbody></table></div></section>");

        html.append("<script>");
        html.append(js());
        html.append("</script></body></html>");
        return html.toString();
    }

    private static String card(String label, String value, String kind) {
        return "<div class=\"card " + kind + "\"><div class=\"label\">" + esc(label)
                + "</div><div class=\"value\">" + esc(value) + "</div></div>";
    }

    private static String failedCard(QueryResult r, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("<article class=\"fail-card\" id=\"fail-").append(index).append("\">");
        sb.append("<h3>").append(esc(display(r.getQuery()))).append("</h3>");
        sb.append("<div class=\"meta-grid\">");
        sb.append(meta("Flow", r.getFlow()));
        sb.append(meta("Language", displayOptional(r.getLanguage())));
        sb.append(meta("Intent", intentDisplay(r)));
        sb.append(meta("Timestamp", display(r.getStartTime())));
        sb.append(meta("Duration", formatDuration(r.getDurationMs())));
        sb.append(meta("Response Validation", displayOptional(r.getResponseValidationStatus())));
        sb.append(meta("Document Validation", displayOptional(r.getDocumentValidationStatus())));
        sb.append(meta("Class", display(r.getTestClass())));
        sb.append(meta("Method", display(r.getTestMethod())));
        sb.append("</div>");
        sb.append("<div class=\"ea\"><div><h4>Expected Response</h4><pre>")
                .append(esc(display(r.getExpectedResponse()))).append("</pre></div>");
        sb.append("<div><h4>Actual Response</h4><pre>")
                .append(esc(display(r.getActualResponse()))).append("</pre></div></div>");
        if (hasText(r.getExpectedDocument()) || hasText(r.getActualDocument())
                || hasText(r.getDocumentValidationStatus())) {
            sb.append("<div class=\"ea\"><div><h4>Expected Document</h4><pre>")
                    .append(esc(displayOptional(r.getExpectedDocument()))).append("</pre></div>");
            sb.append("<div><h4>Actual Document</h4><pre>")
                    .append(esc(displayOptional(r.getActualDocument()))).append("</pre></div></div>");
        }
        sb.append("<h4>Failure reason</h4><pre class=\"err\">")
                .append(esc(display(r.getErrorMessage()))).append("</pre>");
        if (r.getStackTrace() != null && !r.getStackTrace().isBlank()) {
            sb.append("<details><summary>Stack trace</summary><pre class=\"stack\">")
                    .append(esc(r.getStackTrace())).append("</pre></details>");
        }
        if (r.getScreenshotPath() != null && !r.getScreenshotPath().isBlank()) {
            String rel = toReportRelative(r.getScreenshotPath());
            sb.append("<h4>Screenshot</h4>");
            sb.append("<a href=\"").append(esc(rel)).append("\" target=\"_blank\">");
            sb.append("<img class=\"shot\" src=\"").append(esc(rel)).append("\" alt=\"failure screenshot\"/>");
            sb.append("</a>");
        }
        sb.append("</article>");
        return sb.toString();
    }

    private static String resultRow(QueryResult r, int index) {
        String status = display(r.getOverallStatus());
        String flow = display(r.getFlow());
        String lang = displayOptional(r.getLanguage());
        String intent = intentDisplay(r);
        StringBuilder sb = new StringBuilder();
        sb.append("<tr class=\"row\" data-status=\"").append(esc(status)).append("\"")
                .append(" data-flow=\"").append(esc(flow)).append("\"")
                .append(" data-lang=\"").append(esc(lang)).append("\"")
                .append(" data-intent=\"").append(esc(intent)).append("\"")
                .append(" data-query=\"").append(esc(display(r.getQuery())).toLowerCase(Locale.ROOT)).append("\">");
        sb.append("<td><button type=\"button\" class=\"toggle\" data-target=\"detail-")
                .append(index).append("\">+</button></td>");
        sb.append("<td><span class=\"badge ").append(status.toLowerCase(Locale.ROOT)).append("\">")
                .append(esc(status)).append("</span></td>");
        sb.append("<td class=\"q\">").append(esc(display(r.getQuery()))).append("</td>");
        sb.append("<td>").append(esc(flow)).append("</td>");
        sb.append("<td>").append(esc(lang)).append("</td>");
        sb.append("<td>").append(esc(intent)).append("</td>");
        sb.append("<td>").append(esc(display(r.getStartTime()))).append("</td>");
        sb.append("<td>").append(esc(formatDuration(r.getDurationMs()))).append("</td>");
        sb.append("</tr>");
        sb.append("<tr class=\"detail\" id=\"detail-").append(index).append("\" hidden><td colspan=\"8\">");
        sb.append("<div class=\"detail-body\">");
        sb.append(meta("Class", r.getTestClass()));
        sb.append(meta("Method", r.getTestMethod()));
        sb.append(meta("Response Validation", displayOptional(r.getResponseValidationStatus())));
        sb.append(meta("Document Validation", displayOptional(r.getDocumentValidationStatus())));
        sb.append("<div class=\"ea\"><div><h4>Expected Response</h4><pre>")
                .append(esc(display(r.getExpectedResponse()))).append("</pre></div>");
        sb.append("<div><h4>Actual Response</h4><pre>")
                .append(esc(display(r.getActualResponse()))).append("</pre></div></div>");
        if (hasText(r.getExpectedDocument()) || hasText(r.getActualDocument())
                || hasText(r.getDocumentValidationStatus())) {
            sb.append("<div class=\"ea\"><div><h4>Expected Document</h4><pre>")
                    .append(esc(displayOptional(r.getExpectedDocument()))).append("</pre></div>");
            sb.append("<div><h4>Actual Document</h4><pre>")
                    .append(esc(displayOptional(r.getActualDocument()))).append("</pre></div></div>");
        }
        if (r.isFailed()) {
            sb.append("<h4>Failure reason</h4><pre class=\"err\">")
                    .append(esc(display(r.getErrorMessage()))).append("</pre>");
            if (r.getScreenshotPath() != null && !r.getScreenshotPath().isBlank()) {
                String rel = toReportRelative(r.getScreenshotPath());
                sb.append("<h4>Screenshot</h4><a href=\"").append(esc(rel))
                        .append("\" target=\"_blank\"><img class=\"shot\" src=\"")
                        .append(esc(rel)).append("\" alt=\"screenshot\"/></a>");
            }
            if (r.getStackTrace() != null && !r.getStackTrace().isBlank()) {
                sb.append("<details><summary>Stack trace</summary><pre class=\"stack\">")
                        .append(esc(r.getStackTrace())).append("</pre></details>");
            }
        }
        sb.append("</div></td></tr>");
        return sb.toString();
    }

    private static String meta(String label, String value) {
        return "<div class=\"meta\"><span>" + esc(label) + "</span><strong>"
                + esc(display(value)) + "</strong></div>";
    }

    private static String intentDisplay(QueryResult r) {
        if (r.getFlow() != null && r.getFlow().equalsIgnoreCase("Customer Post-Login")) {
            return displayOptional(r.getIntent());
        }
        return "N/A";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String displayOptional(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value;
    }

    private static String display(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String formatDuration(long ms) {
        if (ms <= 0L) {
            return "0s";
        }
        long totalSec = ms / 1000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        if (min > 0) {
            return min + "m " + sec + "s";
        }
        if (totalSec > 0) {
            return totalSec + "s";
        }
        return ms + "ms";
    }

    /** Convert absolute/relative filesystem path to one relative to dashboard-report/. */
    static String toReportRelative(String screenshotPath) {
        String normalized = screenshotPath.replace('\\', '/');
        int idx = normalized.lastIndexOf("target/");
        if (idx >= 0) {
            String fromTarget = normalized.substring(idx);
            if (fromTarget.startsWith("target/")) {
                return "../" + fromTarget.substring("target/".length());
            }
        }
        if (normalized.contains("screenshots/")) {
            int s = normalized.indexOf("screenshots/");
            return "../" + normalized.substring(s);
        }
        return normalized;
    }

    private static String esc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String css() {
        return """
                :root{--bg:#0f172a;--panel:#111827;--card:#1f2937;--text:#e5e7eb;--muted:#94a3b8;
                --pass:#22c55e;--fail:#ef4444;--skip:#f59e0b;--line:#334155;--accent:#38bdf8}
                *{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;
                background:linear-gradient(180deg,#0b1224,#111827 40%,#0f172a);color:var(--text)}
                .hero{display:flex;justify-content:space-between;gap:1rem;padding:1.5rem 2rem;border-bottom:1px solid var(--line)}
                .hero h1{margin:0 0 .25rem;font-size:1.6rem}.sub{margin:0;color:var(--muted)}
                .hero-meta{text-align:right;color:var(--muted);font-size:.92rem}
                .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:1rem;padding:1.25rem 2rem}
                .card{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:1rem}
                .card .label{color:var(--muted);font-size:.85rem}.card .value{font-size:1.5rem;font-weight:700;margin-top:.35rem}
                .card.pass .value{color:var(--pass)}.card.fail .value{color:var(--fail)}.card.skip .value{color:var(--skip)}
                .viz{display:flex;align-items:center;gap:1.5rem;padding:0 2rem 1.25rem}
                .donut{width:110px;height:110px;border-radius:50%;
                background:conic-gradient(var(--pass) calc(var(--p)*1%), var(--fail) 0);
                mask:radial-gradient(circle 40px,transparent 98%,#000 100%);-webkit-mask:radial-gradient(circle 40px,transparent 98%,#000 100%)}
                .viz-legend{display:flex;flex-direction:column;gap:.4rem;color:var(--muted)}
                .dot{display:inline-block;width:.7rem;height:.7rem;border-radius:50%;margin-right:.45rem}
                .dot.pass{background:var(--pass)}.dot.fail{background:var(--fail)}.dot.skip{background:var(--skip)}
                .panel{margin:0 2rem 1.5rem;padding:1.25rem;background:rgba(17,24,39,.85);border:1px solid var(--line);border-radius:16px}
                .panel h2{margin:0 0 1rem}.ok{color:var(--pass)}
                .fail-card{background:var(--card);border:1px solid #7f1d1d;border-radius:12px;padding:1rem;margin-bottom:1rem}
                .fail-card h3{margin:0 0 .75rem;color:#fecaca}
                .meta-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:.6rem;margin-bottom:.8rem}
                .meta{background:#0b1220;border-radius:8px;padding:.55rem .7rem}.meta span{display:block;color:var(--muted);font-size:.75rem}
                .ea{display:grid;grid-template-columns:1fr 1fr;gap:1rem;margin-bottom:.8rem}pre{white-space:pre-wrap;word-break:break-word;
                background:#0b1220;border-radius:8px;padding:.75rem;border:1px solid var(--line);max-height:260px;overflow:auto}
                pre.err{border-color:#7f1d1d;color:#fecaca}pre.stack{max-height:220px;font-size:.8rem}
                .shot{max-width:100%;border-radius:10px;border:1px solid var(--line);margin-top:.4rem}
                .filters{display:flex;flex-wrap:wrap;gap:.6rem;margin-bottom:1rem}
                .filters input,.filters select{background:#0b1220;color:var(--text);border:1px solid var(--line);
                border-radius:8px;padding:.55rem .7rem}
                .table-wrap{overflow:auto}table{width:100%;border-collapse:collapse}
                th,td{padding:.65rem .55rem;border-bottom:1px solid var(--line);text-align:left;vertical-align:top}
                th{color:var(--muted);font-size:.8rem;position:sticky;top:0;background:#111827}
                td.q{max-width:420px}.badge{padding:.2rem .5rem;border-radius:999px;font-size:.75rem;font-weight:700}
                .badge.passed{background:rgba(34,197,94,.15);color:var(--pass)}
                .badge.failed{background:rgba(239,68,68,.15);color:var(--fail)}
                .badge.skipped{background:rgba(245,158,11,.15);color:var(--skip)}
                .toggle{background:#0b1220;color:var(--accent);border:1px solid var(--line);border-radius:6px;cursor:pointer}
                .detail-body{padding:.5rem 0 1rem}
                @media(max-width:800px){.hero{flex-direction:column}.ea{grid-template-columns:1fr}.hero-meta{text-align:left}}
                """;
    }

    private static String js() {
        return """
                const search=document.getElementById('search');
                const statusFilter=document.getElementById('statusFilter');
                const flowFilter=document.getElementById('flowFilter');
                const langFilter=document.getElementById('langFilter');
                const intentFilter=document.getElementById('intentFilter');
                const rows=[...document.querySelectorAll('#resultsTable tbody tr.row')];
                function unique(values){return [...new Set(values.filter(v=>v&&v!=='-'&&v!=='N/A'))].sort();}
                unique(rows.map(r=>r.dataset.lang)).forEach(v=>{const o=document.createElement('option');o.value=v;o.textContent=v;langFilter.appendChild(o);});
                unique(rows.map(r=>r.dataset.intent)).forEach(v=>{const o=document.createElement('option');o.value=v;o.textContent=v;intentFilter.appendChild(o);});
                function applyFilters(){
                  const q=(search.value||'').toLowerCase();
                  const st=statusFilter.value; const fl=flowFilter.value; const lg=langFilter.value; const it=intentFilter.value;
                  rows.forEach(row=>{
                    const match=!q||(row.dataset.query||'').includes(q);
                    const stOk=!st||row.dataset.status===st;
                    const flOk=!fl||row.dataset.flow===fl;
                    const lgOk=!lg||row.dataset.lang===lg;
                    const itOk=!it||row.dataset.intent===it;
                    const show=match&&stOk&&flOk&&lgOk&&itOk;
                    row.style.display=show?'':'none';
                    const detail=document.getElementById(row.querySelector('.toggle').dataset.target);
                    if(detail && !show){detail.hidden=true; row.querySelector('.toggle').textContent='+';}
                  });
                }
                [search,statusFilter,flowFilter,langFilter,intentFilter].forEach(el=>el.addEventListener('input',applyFilters));
                document.querySelectorAll('.toggle').forEach(btn=>{
                  btn.addEventListener('click',()=>{
                    const detail=document.getElementById(btn.dataset.target);
                    if(!detail)return;
                    detail.hidden=!detail.hidden;
                    btn.textContent=detail.hidden?'+':'-';
                  });
                });
                """;
    }
}
