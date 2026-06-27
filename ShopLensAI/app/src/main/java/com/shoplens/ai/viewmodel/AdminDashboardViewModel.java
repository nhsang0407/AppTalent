package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.model.Order;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.repository.OrderRepository;
import com.shoplens.ai.repository.ProductRepository;
import com.shoplens.ai.repository.SearchLogRepository;
import com.shoplens.ai.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboardViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository = new OrderRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final SearchLogRepository searchLogRepository = new SearchLogRepository();

    private final MutableLiveData<Map<String, Object>> dashboardStats = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> lowStockProducts = new MutableLiveData<>();
    private final MutableLiveData<List<Map.Entry<String, Integer>>> topSearches =
            new MutableLiveData<>();
    private final MutableLiveData<List<Order>> recentOrders = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> allOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AdminDashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Map<String, Object>> getDashboardStats() {
        return dashboardStats;
    }

    public MutableLiveData<List<Product>> getLowStockProducts() {
        return lowStockProducts;
    }

    public MutableLiveData<List<Map.Entry<String, Integer>>> getTopSearches() {
        return topSearches;
    }

    public MutableLiveData<List<Order>> getRecentOrders() {
        return recentOrders;
    }

    public MutableLiveData<List<Order>> getAllOrders() {
        return allOrders;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** Loads stats, all orders (for chart + recent), low-stock products and top searches. */
    public void loadDashboard() {
        isLoading.setValue(true);

        orderRepository.getOrderStats(new OrderRepository.StatsCallback() {
            @Override
            public void onSuccess(Map<String, Object> stats) {
                dashboardStats.setValue(stats);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });

        orderRepository.getAllOrders(new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> list) {
                isLoading.setValue(false);
                allOrders.setValue(list);
                int limit = Math.min(list.size(), Constants.RECENT_ORDERS_LIMIT);
                recentOrders.setValue(new ArrayList<>(list.subList(0, limit)));
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });

        productRepository.getAllProducts(null, new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                List<Product> lowStock = new ArrayList<>();
                for (Product p : products) {
                    if (p.getStock() <= Constants.LOW_STOCK_THRESHOLD) {
                        lowStock.add(p);
                    }
                }
                lowStock.sort((a, b) -> Integer.compare(a.getStock(), b.getStock()));
                lowStockProducts.setValue(lowStock);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });

        searchLogRepository.getTopSearchLabels(10, new SearchLogRepository.SearchStatsCallback() {
            @Override
            public void onSuccess(List<Map.Entry<String, Integer>> labels) {
                topSearches.setValue(labels);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
