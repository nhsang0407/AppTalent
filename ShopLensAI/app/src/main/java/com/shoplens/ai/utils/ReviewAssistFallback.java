package com.shoplens.ai.utils;

import com.shoplens.ai.model.Product;

/**
 * Basic rule-based fallback system when ML Kit / Gemini are unavailable.
 */
public class ReviewAssistFallback {

    public static String politeRewrite(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        String cap = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
        if (!cap.endsWith(".") && !cap.endsWith("!") && !cap.endsWith("?")) {
            cap += ".";
        }
        return "Sản phẩm dùng khá ổn, chất lượng tốt trong tầm giá. " + cap + " Mong shop tiếp tục cải thiện và phát triển.";
    }

    public static String shorten(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() > 50) {
            return trimmed.substring(0, 47) + "...";
        }
        return trimmed;
    }

    public static String proofreadBasic(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        String cap = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
        if (!cap.endsWith(".") && !cap.endsWith("!") && !cap.endsWith("?")) {
            cap += ".";
        }
        return cap;
    }

    public static String suggestFromRating(float rating, Product product) {
        String productName = product != null ? product.getName() : "sản phẩm";
        if (rating >= 4) {
            return "Mình khá hài lòng với " + productName + ". Sản phẩm phù hợp với nhu cầu và đáng cân nhắc trong tầm giá.";
        } else if (rating >= 3) {
            return "Mình thấy " + productName + " ở mức ổn. Sản phẩm đáp ứng được nhu cầu cơ bản nhưng vẫn còn điểm có thể cải thiện.";
        } else {
            return "Mình chưa thật sự hài lòng với " + productName + ". Hy vọng shop có thể cải thiện chất lượng hoặc trải nghiệm sử dụng trong thời gian tới.";
        }
    }
}
