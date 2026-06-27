package com.shoplens.ai.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Firestore order document. Stored in the "orders" collection.
 */
public class Order implements Serializable {

    private String orderId;
    private String userId;
    private String userName;
    private String userAddress;
    private List<OrderItem> items;
    private double totalPrice;
    private String status;     // pending | confirmed | done | cancelled
    private Timestamp createdAt;

    public Order() {
        this.items = new ArrayList<>();
    }

    public Order(String userId, String userName, String userAddress,
                 List<OrderItem> items, double totalPrice, String status) {
        this.userId = userId;
        this.userName = userName;
        this.userAddress = userAddress;
        this.items = items;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = Timestamp.now();
    }

    @PropertyName("orderId")
    public String getOrderId() {
        return orderId;
    }

    @PropertyName("orderId")
    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    @PropertyName("userAddress")
    public String getUserAddress() {
        return userAddress;
    }

    @PropertyName("userAddress")
    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    @PropertyName("items")
    public List<OrderItem> getItems() {
        return items;
    }

    @PropertyName("items")
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @PropertyName("totalPrice")
    public double getTotalPrice() {
        return totalPrice;
    }

    @PropertyName("totalPrice")
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalItemCount() {
        int count = 0;
        if (items != null) {
            for (OrderItem item : items) {
                count += item.getQuantity();
            }
        }
        return count;
    }
}
