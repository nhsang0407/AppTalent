package com.shoplens.ai.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.model.CartItem;
import com.shoplens.ai.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Process-wide in-memory cart. A singleton so every Activity's CartViewModel observes the
 * same state (the cart is intentionally not persisted across app restarts).
 */
public final class CartManager {

    private static final CartManager INSTANCE = new CartManager();

    public static CartManager get() {
        return INSTANCE;
    }

    private final MutableLiveData<List<CartItem>> cartItems =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> totalPrice = new MutableLiveData<>(0.0);

    private CartManager() {
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<Double> getTotalPrice() {
        return totalPrice;
    }

    private List<CartItem> items() {
        List<CartItem> list = cartItems.getValue();
        return list != null ? list : new ArrayList<>();
    }

    public void addToCart(Product product, int quantity) {
        List<CartItem> list = items();
        boolean found = false;
        for (CartItem item : list) {
            if (item.getProduct().getProductId().equals(product.getProductId())) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(new CartItem(product, quantity));
        }
        publish(list);
    }

    public void removeFromCart(String productId) {
        List<CartItem> list = items();
        list.removeIf(item -> item.getProduct().getProductId().equals(productId));
        publish(list);
    }

    public void updateQuantity(String productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeFromCart(productId);
            return;
        }
        List<CartItem> list = items();
        for (CartItem item : list) {
            if (item.getProduct().getProductId().equals(productId)) {
                item.setQuantity(newQuantity);
                break;
            }
        }
        publish(list);
    }

    public void clearCart() {
        publish(new ArrayList<>());
    }

    public double getTotalPriceValue() {
        double total = 0;
        for (CartItem item : items()) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartItem item : items()) {
            count += item.getQuantity();
        }
        return count;
    }

    private void publish(List<CartItem> list) {
        cartItems.setValue(list);
        double total = 0;
        for (CartItem item : list) {
            total += item.getSubtotal();
        }
        totalPrice.setValue(total);
    }
}
