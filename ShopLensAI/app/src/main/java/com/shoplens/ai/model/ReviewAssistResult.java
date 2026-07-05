package com.shoplens.ai.model;

import java.io.Serializable;

/**
 * Model wrapper to represent review assistant recommendations.
 */
public class ReviewAssistResult implements Serializable {
    private String suggestedText;
    private String source;
    private boolean fallback;

    public ReviewAssistResult() {
    }

    public ReviewAssistResult(String suggestedText, String source, boolean fallback) {
        this.suggestedText = suggestedText;
        this.source = source;
        this.fallback = fallback;
    }

    public String getSuggestedText() {
        return suggestedText;
    }

    public void setSuggestedText(String suggestedText) {
        this.suggestedText = suggestedText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }
}
