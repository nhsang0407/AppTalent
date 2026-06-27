package com.shoplens.ai.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

/**
 * Firestore review document. Stored in the "reviews" collection.
 */
public class Review implements Serializable {

    private String reviewId;
    private String productId;
    private String userId;
    private String userName;
    private String content;
    private int rating;        // 1..5
    private Timestamp createdAt;

    public Review() {
    }

    public Review(String productId, String userId, String userName, String content, int rating) {
        this.productId = productId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.rating = rating;
        this.createdAt = Timestamp.now();
    }

    @PropertyName("reviewId")
    public String getReviewId() {
        return reviewId;
    }

    @PropertyName("reviewId")
    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    @PropertyName("productId")
    public String getProductId() {
        return productId;
    }

    @PropertyName("productId")
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("userName")
    public String getUserName() {
        return userName;
    }

    @PropertyName("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @PropertyName("content")
    public String getContent() {
        return content;
    }

    @PropertyName("content")
    public void setContent(String content) {
        this.content = content;
    }

    @PropertyName("rating")
    public int getRating() {
        return rating;
    }

    @PropertyName("rating")
    public void setRating(int rating) {
        this.rating = rating;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
