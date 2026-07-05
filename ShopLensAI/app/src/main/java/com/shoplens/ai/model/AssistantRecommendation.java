package com.shoplens.ai.model;

import java.io.Serializable;

/**
 * Holds data parsed from AI product recommendations.
 */
public class AssistantRecommendation implements Serializable {
    private String productId;
    private String productName;
    private String priceText;
    private String stockText;
    private String reason;

    public AssistantRecommendation() {
    }

    public AssistantRecommendation(String productId, String productName, String priceText, String stockText, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.priceText = priceText;
        this.stockText = stockText;
        this.reason = reason;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getPriceText() {
        return priceText;
    }

    public String getStockText() {
        return stockText;
    }

    public String getReason() {
        return reason;
    }
}
