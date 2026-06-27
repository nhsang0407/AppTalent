package com.shoplens.ai.model;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

/**
 * A single line item within an {@link Order}. Embedded in the order document.
 */
public class OrderItem implements Serializable {

    private String productId;
    private String productName;
    private String productImageUrl;
    private double price;
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(String productId, String productName, String productImageUrl,
                     double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.price = price;
        this.quantity = quantity;
    }

    @PropertyName("productId")
    public String getProductId() {
        return productId;
    }

    @PropertyName("productId")
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @PropertyName("productName")
    public String getProductName() {
        return productName;
    }

    @PropertyName("productName")
    public void setProductName(String productName) {
        this.productName = productName;
    }

    @PropertyName("productImageUrl")
    public String getProductImageUrl() {
        return productImageUrl;
    }

    @PropertyName("productImageUrl")
    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    @PropertyName("price")
    public double getPrice() {
        return price;
    }

    @PropertyName("price")
    public void setPrice(double price) {
        this.price = price;
    }

    @PropertyName("quantity")
    public int getQuantity() {
        return quantity;
    }

    @PropertyName("quantity")
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @PropertyName("subtotal")
    public double getSubtotal() {
        return price * quantity;
    }
}
