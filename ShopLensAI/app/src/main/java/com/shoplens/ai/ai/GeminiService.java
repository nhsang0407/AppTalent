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

/**
 * Wraps the Firebase AI Logic (Gemini Developer API) calls used across the app.
 * All callbacks are delivered on the main thread.
 */
public class GeminiService {

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
}
