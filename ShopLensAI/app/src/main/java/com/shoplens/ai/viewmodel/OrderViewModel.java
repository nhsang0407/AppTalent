package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.model.CartItem;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.model.OrderItem;
import com.shoplens.ai.repository.OrderRepository;
import com.shoplens.ai.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository repository = new OrderRepository();

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> orderPlaced = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<Order>> getOrders() {
        return orders;
    }

    public MutableLiveData<Boolean> getOrderPlaced() {
        return orderPlaced;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void placeOrder(List<CartItem> cartItems, String userId, String userName, String address) {
        if (cartItems == null || cartItems.isEmpty()) {
            errorMessage.setValue("Your cart is empty.");
            return;
        }
        isLoading.setValue(true);

        List<OrderItem> items = new ArrayList<>();
        double total = 0;
        for (CartItem ci : cartItems) {
            items.add(new OrderItem(
                    ci.getProduct().getProductId(),
                    ci.getProduct().getName(),
                    ci.getProduct().getDisplayImageUrl(),
                    ci.getProduct().getPrice(),
                    ci.getQuantity()));
            total += ci.getSubtotal();
        }

        Order order = new Order(userId, userName, address, items, total, Constants.STATUS_PENDING);
        repository.createOrder(order, new OrderRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                orderPlaced.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void loadUserOrders(String userId) {
        isLoading.setValue(true);
        repository.getUserOrders(userId, new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> list) {
                isLoading.setValue(false);
                orders.setValue(list);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void loadAllOrders() {
        isLoading.setValue(true);
        repository.getAllOrders(new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> list) {
                isLoading.setValue(false);
                orders.setValue(list);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void updateOrderStatus(String orderId, String status) {
        repository.updateOrderStatus(orderId, status, new OrderRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                loadAllOrders();
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
