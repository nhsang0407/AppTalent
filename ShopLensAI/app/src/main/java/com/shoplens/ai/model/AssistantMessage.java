package com.shoplens.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single message in the AI Shopping Assistant dialogue.
 */
public class AssistantMessage {
    private String content;
    private boolean fromUser;
    private long createdAt;
    private List<AssistantRecommendation> recommendations;

    public AssistantMessage() {
        this.recommendations = new ArrayList<>();
    }

    public AssistantMessage(String content, boolean fromUser, long createdAt) {
        this.content = content;
        this.fromUser = fromUser;
        this.createdAt = createdAt;
        this.recommendations = new ArrayList<>();
    }

    public AssistantMessage(String content, boolean fromUser, long createdAt, List<AssistantRecommendation> recommendations) {
        this.content = content;
        this.fromUser = fromUser;
        this.createdAt = createdAt;
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public String getContent() {
        return content;
    }

    public boolean isFromUser() {
        return fromUser;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<AssistantRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AssistantRecommendation> recommendations) {
        this.recommendations = recommendations;
    }
}
