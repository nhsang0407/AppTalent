package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.model.SearchKeyword;
import com.shoplens.ai.repository.AdminInsightRepository;
import com.shoplens.ai.repository.OrderRepository;
import com.shoplens.ai.repository.ProductRepository;
import com.shoplens.ai.repository.SearchLogRepository;
import com.shoplens.ai.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository = new OrderRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final SearchLogRepository searchLogRepository = new SearchLogRepository();
    private final AdminInsightRepository insightRepository = new AdminInsightRepository();
    private final GeminiService geminiService;

    private final MutableLiveData<Map<String, Object>> dashboardStats = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> lowStockProducts = new MutableLiveData<>();
    private final MutableLiveData<List<Map.Entry<String, Integer>>> topSearches =
            new MutableLiveData<>();
    private final MutableLiveData<List<Order>> recentOrders = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> allOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // AI Insight states
    private final MutableLiveData<String> aiInsight = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingAiInsight = new MutableLiveData<>(false);
    private final MutableLiveData<String> aiInsightError = new MutableLiveData<>();

    private List<Product> allProductsList;
    private boolean aiInsightGenerationPending = false;
    private String fallbackCachePending = null;

    public AdminDashboardViewModel(@NonNull Application application) {
        super(application);
        this.geminiService = new GeminiService(application);
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

    public LiveData<String> getAiInsight() {
        return aiInsight;
    }

    public LiveData<Boolean> getIsLoadingAiInsight() {
        return isLoadingAiInsight;
    }

    public LiveData<String> getAiInsightError() {
        return aiInsightError;
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
                checkAndTriggerAiInsightIfNeeded();
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
                allProductsList = products;
                List<Product> lowStock = new ArrayList<>();
                for (Product p : products) {
                    if (p.getStock() <= Constants.LOW_STOCK_THRESHOLD) {
                        lowStock.add(p);
                    }
                }
                lowStock.sort((a, b) -> java.lang.Integer.compare(a.getStock(), b.getStock()));
                lowStockProducts.setValue(lowStock);
                checkAndTriggerAiInsightIfNeeded();
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
                checkAndTriggerAiInsightIfNeeded();
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void refreshAiInsight(boolean forceRefresh) {
        if (!forceRefresh && aiInsight.getValue() != null) {
            // Already loaded
            return;
        }

        isLoadingAiInsight.setValue(true);
        aiInsightError.setValue(null);

        String dateKey = getCurrentDateKey();

        if (!forceRefresh) {
            // Check cache
            insightRepository.getAdminInsight(dateKey, new AdminInsightRepository.InsightCallback() {
                @Override
                public void onSuccess(String cachedInsight) {
                    if (cachedInsight != null && !cachedInsight.trim().isEmpty()) {
                        aiInsight.setValue(cachedInsight);
                        isLoadingAiInsight.setValue(false);
                    } else {
                        // Cache is empty, start loading dashboard if not already loaded or trigger generation
                        aiInsightGenerationPending = true;
                        fallbackCachePending = null;
                        if (allOrders.getValue() == null || allProductsList == null || topSearches.getValue() == null) {
                            loadDashboard();
                        } else {
                            checkAndTriggerAiInsightIfNeeded();
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Try generating new
                    aiInsightGenerationPending = true;
                    fallbackCachePending = null;
                    if (allOrders.getValue() == null || allProductsList == null || topSearches.getValue() == null) {
                        loadDashboard();
                    } else {
                        checkAndTriggerAiInsightIfNeeded();
                    }
                }
            });
        } else {
            // Force refresh: reload dashboard stats and regenerate AI insight
            // Get old cache in case Gemini fails during regeneration
            insightRepository.getAdminInsight(dateKey, new AdminInsightRepository.InsightCallback() {
                @Override
                public void onSuccess(String cachedInsight) {
                    fallbackCachePending = cachedInsight;
                    aiInsightGenerationPending = true;
                    loadDashboard();
                }

                @Override
                public void onError(Exception e) {
                    fallbackCachePending = null;
                    aiInsightGenerationPending = true;
                    loadDashboard();
                }
            });
        }
    }

    public void refreshAiInsight() {
        refreshAiInsight(false);
    }

    private void checkAndTriggerAiInsightIfNeeded() {
        if (!aiInsightGenerationPending) {
            return;
        }

        List<Product> products = allProductsList;
        List<Order> orders = allOrders.getValue();
        List<Map.Entry<String, Integer>> searches = topSearches.getValue();

        if (products == null || orders == null || searches == null) {
            return; // Not all data loaded yet
        }

        // We have all data, start generating
        aiInsightGenerationPending = false;

        List<SearchKeyword> keywords = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : searches) {
            keywords.add(new SearchKeyword(entry.getKey(), entry.getValue()));
        }

        String dateKey = getCurrentDateKey();

        geminiService.generateAdminInsight(products, orders, keywords, new GeminiService.AdminInsightCallback() {
            @Override
            public void onSuccess(String insight) {
                aiInsight.setValue(insight);
                isLoadingAiInsight.setValue(false);
                aiInsightError.setValue(null);

                // Save to Firestore
                insightRepository.saveAdminInsight(dateKey, insight, null);
            }

            @Override
            public void onError(Exception exception) {
                isLoadingAiInsight.setValue(false);
                if (fallbackCachePending != null && !fallbackCachePending.trim().isEmpty()) {
                    aiInsight.setValue(fallbackCachePending);
                    aiInsightError.setValue("Không thể cập nhật insight mới. Đang hiển thị dữ liệu cũ.");
                } else {
                    aiInsightError.setValue("Lỗi phân tích AI: " + exception.getMessage());
                    aiInsight.setValue("• Chưa thể tải AI Insight lúc này. Vui lòng thử lại sau.\n" +
                            "• Hãy kiểm tra kết nối mạng hoặc cấu hình API Key của Gemini.");
                }
            }
        });
    }

    private String getCurrentDateKey() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return df.format(new Date());
    }
}
