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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public interface ShoppingAssistantCallback {
        void onSuccess(String response);
        void onError(Exception exception);
    }

    public void generateShoppingAssistantResponse(
            @NonNull String userQuery,
            @NonNull List<Product> products,
            @NonNull ShoppingAssistantCallback callback
    ) {
        if (userQuery.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Câu hỏi không được để trống."));
            return;
        }

        List<Product> filteredProducts = selectRelevantProductsForAssistant(userQuery, products);

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Bạn là trợ lý tư vấn mua sắm chuyên nghiệp, thân thiện của ứng dụng ShopLens AI.\n\n");
        promptBuilder.append("Người dùng đang cần tư vấn và đặt câu hỏi sau:\n");
        promptBuilder.append("\"").append(userQuery).append("\"\n\n");
        promptBuilder.append("Dưới đây là danh sách sản phẩm hiện có trong cửa hàng:\n");

        if (filteredProducts.isEmpty()) {
            promptBuilder.append("(Hiện tại cửa hàng chưa có sản phẩm nào phù hợp với yêu cầu của bạn hoặc chưa có dữ liệu)\n");
        } else {
            for (Product p : filteredProducts) {
                promptBuilder.append("- ID: ").append(p.getProductId()).append("\n");
                promptBuilder.append("  Tên: ").append(p.getName()).append("\n");
                promptBuilder.append("  Danh mục: ").append(p.getCategory()).append("\n");
                promptBuilder.append("  Giá: $").append(String.format(Locale.US, "%.2f", p.getPrice())).append("\n");
                promptBuilder.append("  Tồn kho: ").append(p.getStock()).append(" sản phẩm\n");
                promptBuilder.append("  Đánh giá trung bình: ").append(p.getAverageRating()).append("/5 sao\n");
                if (p.getDescription() != null && !p.getDescription().trim().isEmpty()) {
                    promptBuilder.append("  Mô tả: ").append(p.getDescription().trim()).append("\n");
                }
                promptBuilder.append("\n");
            }
        }

        promptBuilder.append("Nhiệm vụ của bạn:\n");
        promptBuilder.append("- Phân tích kỹ nhu cầu mua sắm và câu hỏi của người dùng.\n");
        promptBuilder.append("- Chỉ gợi ý các sản phẩm thực tế có trong danh sách được cung cấp ở trên.\n");
        promptBuilder.append("- Lưu ý đặc biệt: Giá sản phẩm trong danh sách được lưu bằng đơn vị USD ($). Nếu người dùng nói giá tiền bằng VND (ví dụ: 'dưới 500k' hoặc 'dưới 1 triệu'), hãy tự động quy đổi với tỷ giá xấp xỉ 1 USD = 25.000 VND (ví dụ dưới 500k VND tương đương dưới 20 USD, 1 triệu tương đương dưới 40 USD) để chọn lọc sản phẩm cho đúng ngân sách.\n");
        promptBuilder.append("- Ưu tiên các sản phẩm còn hàng (tồn kho > 0). Đối với sản phẩm hết hàng, chỉ gợi ý nếu không còn lựa chọn nào khác và phải nói rõ là sản phẩm này hiện đã hết hàng.\n");
        promptBuilder.append("- Ưu tiên sản phẩm có đánh giá (rating) cao.\n");
        promptBuilder.append("- Nếu người dùng yêu cầu so sánh sản phẩm, hãy so sánh ngắn gọn theo các tiêu chí: giá cả, công dụng/mô tả, rating và tình trạng tồn kho.\n");
        promptBuilder.append("- Không bao giờ bịa đặt (hallucinate) ra sản phẩm, giá cả, rating hoặc số lượng tồn kho không tồn tại trong danh sách.\n");
        promptBuilder.append("- Không bịa productId. Mỗi sản phẩm gợi ý phải giữ nguyên productId từ danh sách được cung cấp.\n");
        promptBuilder.append("- Đối với các yêu cầu liên quan đến da liễu/sức khỏe (ví dụ: skincare cho da dầu): chỉ gợi ý sản phẩm chung phù hợp và luôn thêm câu nhắc nhở an toàn ở phần text bên ngoài (không nằm trong block PRODUCT): \"Bạn nên kiểm tra kỹ thành phần và thử sản phẩm trên một vùng da nhỏ trước khi sử dụng toàn mặt.\"\n");
        promptBuilder.append("- Nếu không có sản phẩm nào phù hợp với yêu cầu, hãy trả lời một cách lịch sự, nói rõ cửa hàng hiện chưa có sản phẩm đáp ứng hoàn toàn nhu cầu này và gợi ý họ thử tìm kiếm theo hướng khác hoặc xem các danh mục khác.\n\n");
        
        promptBuilder.append("Định dạng câu trả lời bắt buộc (phải viết hoàn toàn bằng tiếng Việt, giọng văn thân thiện, súc tích):\n");
        promptBuilder.append("1. Trả lời ngắn gọn, đồng cảm với nhu cầu của người dùng.\n");
        promptBuilder.append("2. Định dạng bắt buộc cho mỗi sản phẩm được gợi ý (3-5 sản phẩm phù hợp nhất) phải được bọc trong các block PRODUCT như sau:\n");
        promptBuilder.append("[PRODUCT]\n");
        promptBuilder.append("id: <productId>\n");
        promptBuilder.append("name: <productName>\n");
        promptBuilder.append("price: <price>\n");
        promptBuilder.append("stock: <stock>\n");
        promptBuilder.append("reason: <lý do phù hợp ngắn gọn>\n");
        promptBuilder.append("[/PRODUCT]\n");
        promptBuilder.append("Chú ý: Không thêm bất kỳ ký tự nào khác bên trong block PRODUCT ngoài 5 trường trên. Trường price hãy hiển thị giá kèm theo USD ($) và nếu muốn hãy mở ngoặc quy đổi sang VND xấp xỉ.\n");
        promptBuilder.append("3. Kết thúc bằng một câu hỏi gợi mở, thân thiện để tiếp tục hỗ trợ người dùng.\n");

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

    public static List<Product> selectRelevantProductsForAssistant(String userQuery, List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        String queryLower = userQuery.toLowerCase(Locale.ROOT);
        double budgetUsd = -1;

        try {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(k|triệu|tr|usd|\\$|đô|đ|vnd)?");
            Matcher matcher = pattern.matcher(queryLower);
            
            boolean hasConstraint = queryLower.contains("dưới") || queryLower.contains("tầm") || 
                                     queryLower.contains("khoảng") || queryLower.contains("under") || 
                                     queryLower.contains("below") || queryLower.contains("<");

            if (hasConstraint) {
                while (matcher.find()) {
                    double val = Double.parseDouble(matcher.group(1));
                    String unit = matcher.group(3);
                    if (unit != null) {
                        if (unit.equals("k")) {
                            budgetUsd = (val * 1000) / 25000.0;
                        } else if (unit.equals("triệu") || unit.equals("tr")) {
                            budgetUsd = (val * 1000000) / 25000.0;
                        } else if (unit.equals("usd") || unit.equals("$") || unit.equals("đô")) {
                            budgetUsd = val;
                        } else if (unit.equals("vnd") || unit.equals("đ")) {
                            budgetUsd = val / 25000.0;
                        }
                    } else {
                        if (val >= 1000) {
                            budgetUsd = val / 25000.0;
                        } else {
                            budgetUsd = val;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        List<String> stopWords = Arrays.asList("tôi", "cần", "tìm", "mua", "cho", "ở", "trong", "dưới", "khoảng", "tầm", 
                                              "đồ", "cái", "chiếc", "sản", "phẩm", "này", "kia", "và", "hoặc", "có", "không", "giúp");

        String[] words = queryLower.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String w : words) {
            String clean = w.replaceAll("[^a-zA-Z0-9áàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ]", "");
            if (clean.length() > 1 && !stopWords.contains(clean)) {
                keywords.add(clean);
            }
        }

        List<ProductScore> scoredProducts = new ArrayList<>();
        for (Product p : products) {
            double score = 0;

            if (budgetUsd > 0) {
                if (p.getPrice() <= budgetUsd) {
                    score += 50;
                } else {
                    score -= 50;
                }
            }

            String pName = p.getName().toLowerCase(Locale.ROOT);
            String pCat = p.getCategory().toLowerCase(Locale.ROOT);
            String pDesc = p.getDescription() != null ? p.getDescription().toLowerCase(Locale.ROOT) : "";

            for (String kw : keywords) {
                if (pName.contains(kw)) {
                    score += 15;
                }
                if (pCat.contains(kw)) {
                    score += 8;
                }
                if (pDesc.contains(kw)) {
                    score += 4;
                }
                if (p.getTags() != null) {
                    for (String tag : p.getTags()) {
                        if (tag.toLowerCase(Locale.ROOT).contains(kw)) {
                            score += 5;
                            break;
                        }
                    }
                }
            }

            if (p.getStock() > 0) {
                score += 5;
            } else {
                score -= 20;
            }

            score += p.getAverageRating() * 2;

            scoredProducts.add(new ProductScore(p, score));
        }

        scoredProducts.sort((a, b) -> Double.compare(b.score, a.score));

        List<Product> result = new ArrayList<>();
        int limit = Math.min(scoredProducts.size(), 30);
        for (int i = 0; i < limit; i++) {
            result.add(scoredProducts.get(i).product);
        }

        return result;
    }

    private static class ProductScore {
        Product product;
        double score;

        ProductScore(Product product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}
