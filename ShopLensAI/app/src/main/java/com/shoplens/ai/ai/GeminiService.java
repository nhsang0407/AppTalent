package com.shoplens.ai.ai;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.ImagePart;
import com.google.firebase.ai.type.Part;
import com.google.firebase.ai.type.ResponseModality;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import com.shoplens.ai.model.Order;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.model.SearchKeyword;

/**
 * Wraps the Firebase AI Logic (Gemini Developer API) calls used across the app.
 * All callbacks are delivered on the main thread.
 */
public class GeminiService {

    public interface AdminInsightCallback {
        void onSuccess(String insight);
        void onError(Exception exception);
    }


    /** Text/multimodal model. */
    private static final String TEXT_MODEL = "gemini-2.5-flash";
    /**
     * Image-generation model ("Nano Banana"). If this preview id is rejected by your project,
     * switch it to the image-capable model enabled in your Firebase AI Logic console.
     */
    private static final String IMAGE_MODEL = "gemini-2.5-flash-image-preview";

    public interface GeminiCallback<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }

    private final Executor mainExecutor;
    private final GenerativeModelFutures textModel;

    public GeminiService(@NonNull Context context) {
        this.mainExecutor = ContextCompat.getMainExecutor(context.getApplicationContext());
        GenerativeModel model = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(TEXT_MODEL);
        this.textModel = GenerativeModelFutures.from(model);
    }

    // ---------------------------------------------------------------------
    // Visual search: image -> JSON description + category
    // ---------------------------------------------------------------------
    public void analyzeProductImage(@NonNull Bitmap image, @NonNull GeminiCallback<String> callback) {
        String prompt = "You are a retail product identifier. Analyze this image and return a JSON "
                + "with: {productName: string, category: string, description: string, "
                + "searchKeywords: [string]}. Be concise and focus on identifiable product features.";
        Content content = new Content.Builder()
                .addText(prompt)
                .addImage(image)
                .build();
        runText(textModel.generateContent(content), callback);
    }

    // ---------------------------------------------------------------------
    // Summarize reviews
    // ---------------------------------------------------------------------
    public void summarizeReviews(@NonNull List<String> reviewTexts,
                                 @NonNull GeminiCallback<String> callback) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < reviewTexts.size(); i++) {
            joined.append(i + 1).append(". ").append(reviewTexts.get(i)).append('\n');
        }
        String prompt = "Summarize these customer reviews in bullet points. "
                + "Format: ✅ Pros: [positive points] | ❌ Cons: [negative points]. "
                + "Keep it under 100 words.\n\nReviews:\n" + joined;
        Content content = new Content.Builder().addText(prompt).build();
        runText(textModel.generateContent(content), callback);
    }

    // ---------------------------------------------------------------------
    // Generate product description
    // ---------------------------------------------------------------------
    public void generateProductDescription(@NonNull String productName, @NonNull String category,
                                           @NonNull GeminiCallback<String> callback) {
        String prompt = "Write a compelling 2-sentence product description for a retail app. "
                + "Product: " + productName + ", Category: " + category
                + ". Be persuasive and highlight key benefits.";
        Content content = new Content.Builder().addText(prompt).build();
        runText(textModel.generateContent(content), callback);
    }

    // ---------------------------------------------------------------------
    // Generate product image (Gemini image generation)
    // ---------------------------------------------------------------------
    public void generateProductImage(@NonNull String productName, @NonNull String description,
                                     @NonNull GeminiCallback<Bitmap> callback) {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseModalities =
                Arrays.asList(ResponseModality.TEXT, ResponseModality.IMAGE);
        GenerationConfig config = configBuilder.build();

        GenerativeModel imageGenModel = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(IMAGE_MODEL, config);
        GenerativeModelFutures imageModel = GenerativeModelFutures.from(imageGenModel);

        String prompt = "Generate a clean, professional product photo on white background for: "
                + productName + ". " + description
                + ". Style: e-commerce product listing photo, high quality, no text overlay.";
        Content content = new Content.Builder().addText(prompt).build();

        ListenableFuture<GenerateContentResponse> future = imageModel.generateContent(content);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                Bitmap bitmap = extractImage(result);
                if (bitmap != null) {
                    callback.onSuccess(bitmap);
                } else {
                    callback.onError(new IllegalStateException(
                            "The model did not return an image."));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(asException(t));
            }
        }, mainExecutor);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private void runText(@NonNull ListenableFuture<GenerateContentResponse> future,
                         @NonNull GeminiCallback<String> callback) {
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.trim().isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError(new IllegalStateException("Empty response from Gemini."));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(asException(t));
            }
        }, mainExecutor);
    }

    private static Bitmap extractImage(GenerateContentResponse result) {
        if (result == null || result.getCandidates() == null || result.getCandidates().isEmpty()) {
            return null;
        }
        List<Part> parts = result.getCandidates().get(0).getContent().getParts();
        for (Part part : parts) {
            if (part instanceof ImagePart) {
                return ((ImagePart) part).getImage();
            }
        }
        return null;
    }

    private static Exception asException(Throwable t) {
        return (t instanceof Exception) ? (Exception) t : new Exception(t);
    }

    public void generateAdminInsight(
            @NonNull List<Product> products,
            @NonNull List<Order> orders,
            @NonNull List<SearchKeyword> topSearches,
            @NonNull AdminInsightCallback callback
    ) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Bạn là trợ lý phân tích kinh doanh cho admin của một app thương mại điện tử.\n\n");
        promptBuilder.append("Dựa trên dữ liệu thống kê cửa hàng sau đây:\n\n");

        // 1. Products & Inventory Info
        promptBuilder.append("- DANH SÁCH SẢN PHẨM & TỒN KHO:\n");
        if (products.isEmpty()) {
            promptBuilder.append("  (Chưa có sản phẩm nào trong hệ thống)\n");
        } else {
            promptBuilder.append("  Tổng số sản phẩm: ").append(products.size()).append("\n");
            promptBuilder.append("  Sản phẩm tồn kho thấp (dưới hoặc bằng 5 sản phẩm):\n");
            int lowStockCount = 0;
            for (Product p : products) {
                if (p.getStock() <= 5) {
                    promptBuilder.append("    * ").append(p.getName())
                                 .append(" (Tồn kho: ").append(p.getStock())
                                 .append(", Danh mục: ").append(p.getCategory()).append(")\n");
                    lowStockCount++;
                    if (lowStockCount >= 10) {
                        promptBuilder.append("    * ... và một số sản phẩm khác.\n");
                        break;
                    }
                }
            }
            if (lowStockCount == 0) {
                promptBuilder.append("    * Không có sản phẩm nào sắp hết hàng.\n");
            }
        }
        promptBuilder.append("\n");

        // 2. Orders Info
        promptBuilder.append("- DANH SÁCH ĐƠN HÀNG:\n");
        if (orders.isEmpty()) {
            promptBuilder.append("  (Chưa có đơn hàng nào trong hệ thống)\n");
        } else {
            promptBuilder.append("  Tổng số đơn hàng: ").append(orders.size()).append("\n");
            int pendingCount = 0;
            int confirmedCount = 0;
            int doneCount = 0;
            double pendingValue = 0.0;
            double totalRevenue = 0.0;
            for (Order o : orders) {
                if ("pending".equals(o.getStatus())) {
                    pendingCount++;
                    pendingValue += o.getTotalPrice();
                } else if ("confirmed".equals(o.getStatus())) {
                    confirmedCount++;
                    totalRevenue += o.getTotalPrice();
                } else if ("done".equals(o.getStatus())) {
                    doneCount++;
                    totalRevenue += o.getTotalPrice();
                }
            }
            promptBuilder.append("  * Đơn hàng chờ xử lý (pending): ").append(pendingCount)
                         .append(" đơn, Tổng giá trị chờ: $").append(String.format(java.util.Locale.US, "%.2f", pendingValue)).append("\n");
            promptBuilder.append("  * Đơn hàng đã xác nhận/hoàn thành: ").append(confirmedCount + doneCount)
                         .append(" đơn, Tổng doanh thu ước tính: $").append(String.format(java.util.Locale.US, "%.2f", totalRevenue)).append("\n");
        }
        promptBuilder.append("\n");

        // 3. Top Searches Info
        promptBuilder.append("- TOP TỪ KHÓA TÌM KIẾM CỦA NGƯỜI DÙNG:\n");
        if (topSearches.isEmpty()) {
            promptBuilder.append("  (Chưa có từ khóa nào được tìm kiếm nhiều)\n");
        } else {
            for (SearchKeyword sk : topSearches) {
                promptBuilder.append("  * ").append(sk.getKeyword())
                             .append(" (Tìm kiếm: ").append(sk.getCount()).append(" lần)\n");
            }
        }
        promptBuilder.append("\n");

        promptBuilder.append("Yêu cầu output:\n");
        promptBuilder.append("- Tạo báo cáo insight ngắn gọn bằng tiếng Việt cho admin.\n");
        promptBuilder.append("- Định dạng: Viết từ 4 đến 7 dấu đầu dòng (bullet points) rõ ràng, súc tích.\n");
        promptBuilder.append("- Mỗi dấu đầu dòng phải chỉ ra một nhận định hoặc đề xuất hành động kinh doanh thực tế.\n");
        promptBuilder.append("- Tập trung vào: từ khóa đang được tìm nhiều, sản phẩm hot bị tồn kho thấp, danh mục cần bổ sung hàng, các đơn hàng cần ưu tiên xử lý, hoặc gợi ý khuyến mãi.\n");
        promptBuilder.append("- Giọng văn chuyên nghiệp, ngắn gọn, dễ hiểu.\n");
        promptBuilder.append("- Không bịa đặt số liệu. Nếu thiếu dữ liệu, hãy báo \"chưa đủ dữ liệu\" và đưa ra gợi ý chung hợp lý.\n");

        Content content = new Content.Builder().addText(promptBuilder.toString()).build();
        
        Futures.addCallback(textModel.generateContent(content), new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.trim().isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError(new IllegalStateException("Empty response from Gemini."));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(asException(t));
            }
        }, mainExecutor);
    }
}
