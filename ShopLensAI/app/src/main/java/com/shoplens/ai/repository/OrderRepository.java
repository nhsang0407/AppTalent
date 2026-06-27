package com.shoplens.ai.repository;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the "orders" collection.
 */
public class OrderRepository {

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public interface OrderListCallback {
        void onSuccess(List<Order> orders);

        void onError(Exception e);
    }

    public interface StatsCallback {
        void onSuccess(Map<String, Object> stats);

        void onError(Exception e);
    }

    public void createOrder(Order order, @NonNull VoidCallback callback) {
        DocumentReference ref =
                FirebaseUtils.getDb().collection(Constants.COLLECTION_ORDERS).document();
        order.setOrderId(ref.getId());
        ref.set(order)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getUserOrders(String userId, @NonNull OrderListCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        orders.add(o);
                    });
                    sortByDateDesc(orders);
                    callback.onSuccess(orders);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAllOrders(@NonNull OrderListCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_ORDERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        orders.add(o);
                    });
                    callback.onSuccess(orders);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateOrderStatus(String orderId, String newStatus, @NonNull VoidCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_ORDERS).document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Aggregates dashboard statistics: totalRevenue, totalOrders, pendingOrders, todayRevenue.
     * Revenue counts only non-cancelled orders.
     */
    public void getOrderStats(@NonNull StatsCallback callback) {
        getAllOrders(new OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                double totalRevenue = 0;
                double todayRevenue = 0;
                int pendingOrders = 0;
                long startOfToday = startOfTodayMillis();

                for (Order o : orders) {
                    boolean cancelled = Constants.STATUS_CANCELLED.equals(o.getStatus());
                    if (!cancelled) {
                        totalRevenue += o.getTotalPrice();
                        if (o.getCreatedAt() != null
                                && o.getCreatedAt().toDate().getTime() >= startOfToday) {
                            todayRevenue += o.getTotalPrice();
                        }
                    }
                    if (Constants.STATUS_PENDING.equals(o.getStatus())) {
                        pendingOrders++;
                    }
                }

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalRevenue", totalRevenue);
                stats.put("totalOrders", orders.size());
                stats.put("pendingOrders", pendingOrders);
                stats.put("todayRevenue", todayRevenue);
                callback.onSuccess(stats);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private static long startOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static void sortByDateDesc(List<Order> orders) {
        orders.sort((a, b) -> {
            Timestamp ta = a.getCreatedAt();
            Timestamp tb = b.getCreatedAt();
            Date da = ta != null ? ta.toDate() : new Date(0);
            Date db = tb != null ? tb.toDate() : new Date(0);
            return db.compareTo(da);
        });
    }
}
