package com.shoplens.ai.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Firestore product document. Stored in the "products" collection.
 */
public class Product implements Serializable {

    private String productId;
    private String name;
    private String description;
    private String category;
    private String barcode;
    private String imageUrl;
    private String aiGeneratedImageUrl;
    private double price;
    private int stock;
    private List<String> tags;
    private Timestamp createdAt;
    private double averageRating;
    private int reviewCount;
    /** Cached AI summary of reviews (optional). */
    private String reviewSummary;

    public Product() {
        this.tags = new ArrayList<>();
    }

    public Product(String name, String description, String category, double price, int stock) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.tags = new ArrayList<>();
        this.barcode = "";
        this.imageUrl = "";
        this.aiGeneratedImageUrl = "";
        this.averageRating = 0;
        this.reviewCount = 0;
        this.createdAt = Timestamp.now();
    }

    @PropertyName("productId")
    public String getProductId() {
        return productId;
    }

    @PropertyName("productId")
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("category")
    public String getCategory() {
        return category;
    }

    @PropertyName("category")
    public void setCategory(String category) {
        this.category = category;
    }

    @PropertyName("barcode")
    public String getBarcode() {
        return barcode;
    }

    @PropertyName("barcode")
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @PropertyName("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("aiGeneratedImageUrl")
    public String getAiGeneratedImageUrl() {
        return aiGeneratedImageUrl;
    }

    @PropertyName("aiGeneratedImageUrl")
    public void setAiGeneratedImageUrl(String aiGeneratedImageUrl) {
        this.aiGeneratedImageUrl = aiGeneratedImageUrl;
    }

    @PropertyName("price")
    public double getPrice() {
        return price;
    }

    @PropertyName("price")
    public void setPrice(double price) {
        this.price = price;
    }

    @PropertyName("stock")
    public int getStock() {
        return stock;
    }

    @PropertyName("stock")
    public void setStock(int stock) {
        this.stock = stock;
    }

    @PropertyName("tags")
    public List<String> getTags() {
        return tags;
    }

    @PropertyName("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("averageRating")
    public double getAverageRating() {
        return averageRating;
    }

    @PropertyName("averageRating")
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    @PropertyName("reviewCount")
    public int getReviewCount() {
        return reviewCount;
    }

    @PropertyName("reviewCount")
    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    @PropertyName("reviewSummary")
    public String getReviewSummary() {
        return reviewSummary;
    }

    @PropertyName("reviewSummary")
    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    /** Convenience: returns the best display image (real photo first, then AI image). */
    public String getDisplayImageUrl() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }
        return aiGeneratedImageUrl;
    }

    public boolean isOutOfStock() {
        return stock <= 0;
    }

    public boolean isLowStock() {
        return stock <= 5;
    }
}
