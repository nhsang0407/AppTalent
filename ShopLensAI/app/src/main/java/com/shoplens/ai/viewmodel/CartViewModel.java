package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.shoplens.ai.model.CartItem;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.repository.CartManager;

import java.util.List;

/**
 * Thin ViewModel over the process-wide {@link CartManager} so the cart is shared across activities.
 */
public class CartViewModel extends AndroidViewModel {

    private final CartManager cart = CartManager.get();

    public CartViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cart.getCartItems();
    }

    public LiveData<Double> getTotalPriceLiveData() {
        return cart.getTotalPrice();
    }

    public void addToCart(Product product, int quantity) {
        cart.addToCart(product, quantity);
    }

    public void removeFromCart(String productId) {
        cart.removeFromCart(productId);
    }

    public void updateQuantity(String productId, int newQuantity) {
        cart.updateQuantity(productId, newQuantity);
    }

    public void clearCart() {
        cart.clearCart();
    }

    public double getTotalPrice() {
        return cart.getTotalPriceValue();
    }

    public int getTotalItemCount() {
        return cart.getTotalItemCount();
    }
}
