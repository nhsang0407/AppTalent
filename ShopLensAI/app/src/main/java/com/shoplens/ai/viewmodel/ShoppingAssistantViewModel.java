package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.model.AssistantMessage;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class ShoppingAssistantViewModel extends AndroidViewModel {

    private final ProductRepository repository = new ProductRepository();
    private final GeminiService geminiService;

    private final MutableLiveData<List<AssistantMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private List<Product> cachedProductsList = null;

    public ShoppingAssistantViewModel(@NonNull Application application) {
        super(application);
        this.geminiService = new GeminiService(application);
        
        // Add introductory message
        List<AssistantMessage> initList = new ArrayList<>();
        initList.add(new AssistantMessage("Chào bạn! Mình là trợ lý mua sắm AI. Bạn muốn tìm kiếm sản phẩm nào hôm nay? Hãy nhập nhu cầu của bạn (ví dụ: 'quà tặng dưới 500k', 'đồ skincare cho da dầu', v.v.), mình sẽ gợi ý sản phẩm phù hợp nhất.", false, System.currentTimeMillis()));
        messages.setValue(initList);
    }

    public LiveData<List<AssistantMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void sendQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return;
        }

        // Add user question to message log
        List<AssistantMessage> currentMessages = messages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        currentMessages.add(new AssistantMessage(question, true, System.currentTimeMillis()));
        messages.setValue(currentMessages);

        isLoading.setValue(true);
        errorMessage.setValue(null);

        // Load catalog and proceed
        if (cachedProductsList != null && !cachedProductsList.isEmpty()) {
            callAssistantAPI(question, cachedProductsList);
        } else {
            repository.getAllProducts(null, new ProductRepository.ProductListCallback() {
                @Override
                public void onSuccess(List<Product> products) {
                    cachedProductsList = products;
                    if (products == null || products.isEmpty()) {
                        isLoading.setValue(false);
                        List<AssistantMessage> list = messages.getValue();
                        if (list != null) {
                            list.add(new AssistantMessage("Hiện chưa có đủ dữ liệu sản phẩm để tư vấn. Vui lòng quay lại sau.", false, System.currentTimeMillis()));
                            messages.setValue(list);
                        }
                    } else {
                        callAssistantAPI(question, products);
                    }
                }

                @Override
                public void onError(Exception e) {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                    List<AssistantMessage> list = messages.getValue();
                    if (list != null) {
                        list.add(new AssistantMessage("Hiện tại mình chưa thể tải được danh sách sản phẩm. Bạn có thể thử lại sau.", false, System.currentTimeMillis()));
                        messages.setValue(list);
                    }
                }
            });
        }
    }

    private void callAssistantAPI(String question, List<Product> products) {
        geminiService.generateShoppingAssistantResponse(question, products, new GeminiService.ShoppingAssistantCallback() {
            @Override
            public void onSuccess(String response) {
                isLoading.setValue(false);
                List<AssistantMessage> list = messages.getValue();
                if (list == null) {
                    list = new ArrayList<>();
                }

                // 1. Parse raw recommendations and conversational text
                List<com.shoplens.ai.model.AssistantRecommendation> rawRecs = 
                        com.shoplens.ai.utils.AssistantRecommendationParser.parseRecommendations(response);
                String cleanText = com.shoplens.ai.utils.AssistantRecommendationParser.removeProductBlocks(response);
                
                // Strip markdown from the text
                cleanText = com.shoplens.ai.utils.TextFormatUtils.stripBasicMarkdown(cleanText);

                // 2. Validate productIds against store catalog
                List<com.shoplens.ai.model.AssistantRecommendation> validRecs = new ArrayList<>();
                for (com.shoplens.ai.model.AssistantRecommendation rec : rawRecs) {
                    boolean exists = false;
                    for (Product p : products) {
                        if (p.getProductId().equals(rec.getProductId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        validRecs.add(rec);
                    }
                }

                // 3. Fallback to raw response if no recommendations could be parsed or validated
                if (validRecs.isEmpty() && rawRecs.isEmpty()) {
                    String formattedRaw = com.shoplens.ai.utils.TextFormatUtils.stripBasicMarkdown(response);
                    list.add(new AssistantMessage(formattedRaw, false, System.currentTimeMillis()));
                } else {
                    list.add(new AssistantMessage(cleanText, false, System.currentTimeMillis(), validRecs));
                }
                messages.setValue(list);
            }

            @Override
            public void onError(Exception exception) {
                isLoading.setValue(false);
                errorMessage.setValue(exception.getMessage());
                List<AssistantMessage> list = messages.getValue();
                if (list == null) {
                    list = new ArrayList<>();
                }

                // Create fallback recommendations from local matching candidate products
                List<Product> candidates = GeminiService.selectRelevantProductsForAssistant(question, products);
                List<com.shoplens.ai.model.AssistantRecommendation> fallbackRecs = new ArrayList<>();
                int limit = Math.min(candidates.size(), 3);
                for (int i = 0; i < limit; i++) {
                    Product p = candidates.get(i);
                    fallbackRecs.add(new com.shoplens.ai.model.AssistantRecommendation(
                        p.getProductId(),
                        p.getName(),
                        "$" + String.format(java.util.Locale.US, "%.2f", p.getPrice()),
                        "Tồn kho: " + p.getStock() + " sản phẩm",
                        "Sản phẩm phù hợp gợi ý tự động từ cửa hàng."
                    ));
                }

                list.add(new AssistantMessage(
                    "AI hiện chưa phản hồi được, nhưng mình tìm thấy một vài sản phẩm có thể phù hợp:",
                    false,
                    System.currentTimeMillis(),
                    fallbackRecs
                ));
                messages.setValue(list);
            }
        });
    }
}
