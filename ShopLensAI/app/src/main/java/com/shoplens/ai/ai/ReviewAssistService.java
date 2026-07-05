package com.shoplens.ai.ai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shoplens.ai.model.Product;
import com.shoplens.ai.model.ReviewAssistAction;
import com.shoplens.ai.model.ReviewAssistResult;
import com.shoplens.ai.utils.ReviewAssistFallback;

/**
 * Service to orchestrate the AI Review Assistant, using Gemini Cloud or Local Fallbacks.
 */
public class ReviewAssistService {

    private final GeminiService geminiService;

    public interface ReviewAssistCallback {
        void onSuccess(ReviewAssistResult result);
        void onError(Exception exception);
    }

    public ReviewAssistService(Context context) {
        this.geminiService = new GeminiService(context);
    }

    public void assistReview(
            @NonNull ReviewAssistAction action,
            @Nullable String currentReviewText,
            float rating,
            @Nullable Product product,
            @NonNull ReviewAssistCallback callback
    ) {
        // Detect if ML Kit GenAI or another on-device generator is available.
        // Standard ML Kit SmartReply is not suitable for rewriting or summary.
        // So on-device is not available.
        boolean onDeviceAvailable = false;

        if (onDeviceAvailable) {
            // We would call ML Kit on-device model here.
        } else {
            // Call Gemini Cloud
            geminiService.generateReviewAssistText(action, currentReviewText, rating, product, new GeminiService.GeminiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(new ReviewAssistResult(result, "gemini_cloud", false));
                }

                @Override
                public void onError(Exception e) {
                    // Fallback to local
                    String text = getLocalFallbackText(action, currentReviewText, rating, product);
                    callback.onSuccess(new ReviewAssistResult(text, "local_fallback", true));
                }
            });
        }
    }

    private String getLocalFallbackText(ReviewAssistAction action, String text, float rating, Product product) {
        switch (action) {
            case POLITE_REWRITE:
                return ReviewAssistFallback.politeRewrite(text);
            case SHORTEN:
                return ReviewAssistFallback.shorten(text);
            case PROOFREAD:
                return ReviewAssistFallback.proofreadBasic(text);
            case SUGGEST_FROM_RATING:
                return ReviewAssistFallback.suggestFromRating(rating, product);
            default:
                return "";
        }
    }
}
