package com.shoplens.ai.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Firestore search log document. Stored in the "searchLogs" collection.
 */
public class SearchLog {

    private String logId;
    private String userId;
    private String searchType;     // "barcode" | "visual"
    private String detectedLabel;
    private Timestamp timestamp;

    public SearchLog() {
    }

    public SearchLog(String userId, String searchType, String detectedLabel) {
        this.userId = userId;
        this.searchType = searchType;
        this.detectedLabel = detectedLabel;
        this.timestamp = Timestamp.now();
    }

    @PropertyName("logId")
    public String getLogId() {
        return logId;
    }

    @PropertyName("logId")
    public void setLogId(String logId) {
        this.logId = logId;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("searchType")
    public String getSearchType() {
        return searchType;
    }

    @PropertyName("searchType")
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    @PropertyName("detectedLabel")
    public String getDetectedLabel() {
        return detectedLabel;
    }

    @PropertyName("detectedLabel")
    public void setDetectedLabel(String detectedLabel) {
        this.detectedLabel = detectedLabel;
    }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
