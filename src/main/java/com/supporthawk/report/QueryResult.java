package com.supporthawk.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One executed query/question result for the SupportHawk HTML dashboard.
 * Intent/language/document fields are optional; use null when not applicable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryResult {

    public enum Status {
        PASSED, FAILED, SKIPPED, N_A
    }

    private String overallStatus;
    private String query;
    private String flow;
    private String testClass;
    private String testMethod;
    private String language;
    private String intent;
    private String startTime;
    private String endTime;
    private long durationMs;
    private String expectedResponse;
    private String actualResponse;
    private String responseValidationStatus;
    private String expectedDocument;
    private String actualDocument;
    private String documentValidationStatus;
    private String errorMessage;
    private String stackTrace;
    private String screenshotPath;

    public QueryResult() {
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    /** @deprecated use {@link #getOverallStatus()} */
    @JsonIgnore
    public String getStatus() {
        return overallStatus;
    }

    /** @deprecated use {@link #setOverallStatus(String)} */
    @JsonIgnore
    public void setStatus(String status) {
        this.overallStatus = status;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getTestClass() {
        return testClass;
    }

    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public String getActualResponse() {
        return actualResponse;
    }

    public void setActualResponse(String actualResponse) {
        this.actualResponse = actualResponse;
    }

    public String getResponseValidationStatus() {
        return responseValidationStatus;
    }

    public void setResponseValidationStatus(String responseValidationStatus) {
        this.responseValidationStatus = responseValidationStatus;
    }

    public String getExpectedDocument() {
        return expectedDocument;
    }

    public void setExpectedDocument(String expectedDocument) {
        this.expectedDocument = expectedDocument;
    }

    public String getActualDocument() {
        return actualDocument;
    }

    public void setActualDocument(String actualDocument) {
        this.actualDocument = actualDocument;
    }

    public String getDocumentValidationStatus() {
        return documentValidationStatus;
    }

    public void setDocumentValidationStatus(String documentValidationStatus) {
        this.documentValidationStatus = documentValidationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    @JsonIgnore
    public boolean isFailed() {
        return Status.FAILED.name().equalsIgnoreCase(overallStatus);
    }

    @JsonIgnore
    public boolean isPassed() {
        return Status.PASSED.name().equalsIgnoreCase(overallStatus);
    }

    @JsonIgnore
    public boolean isSkipped() {
        return Status.SKIPPED.name().equalsIgnoreCase(overallStatus);
    }

    public void refreshOverallStatus() {
        boolean responseFailed = Status.FAILED.name().equalsIgnoreCase(responseValidationStatus);
        boolean documentFailed = Status.FAILED.name().equalsIgnoreCase(documentValidationStatus);
        if (responseFailed || documentFailed) {
            overallStatus = Status.FAILED.name();
        } else if (overallStatus == null || overallStatus.isBlank()) {
            overallStatus = Status.PASSED.name();
        }
    }
}
