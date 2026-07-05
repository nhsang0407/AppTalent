package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.model.Review;
import com.shoplens.ai.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

public class ReviewViewModel extends AndroidViewModel {

    private final ReviewRepository repository = new ReviewRepository();
    private final GeminiService geminiService;

    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>();
    private final MutableLiveData<String> aiSummary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> summaryLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> reviewAdded = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReviewAssistLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> reviewAssistSuggestion = new MutableLiveData<>();
    private final MutableLiveData<String> reviewAssistError = new MutableLiveData<>();

    public ReviewViewModel(@NonNull Application application) {
        super(application);
        this.geminiService = new GeminiService(application);
    }

    public MutableLiveData<List<Review>> getReviews() {
        return reviews;
    }

    public MutableLiveData<String> getAiSummary() {
        return aiSummary;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<Boolean> getSummaryLoading() {
        return summaryLoading;
    }

    public MutableLiveData<Boolean> getReviewAdded() {
        return reviewAdded;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<Boolean> getIsReviewAssistLoading() {
        return isReviewAssistLoading;
    }

    public MutableLiveData<String> getReviewAssistSuggestion() {
        return reviewAssistSuggestion;
    }

    public MutableLiveData<String> getReviewAssistError() {
        return reviewAssistError;
    }

    public void loadReviews(String productId) {
        isLoading.setValue(true);
        repository.getReviewsByProduct(productId, new ReviewRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> list) {
                isLoading.setValue(false);
                reviews.setValue(list);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void addReview(String productId, String userId, String userName,
                          String content, int rating) {
        Review review = new Review(productId, userId, userName, content, rating);
        repository.addReview(review, new ReviewRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                reviewAdded.setValue(true);
                loadReviews(productId);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    /** Use the cached AI summary if present; otherwise generate one with Gemini and cache it. */
    public void loadOrGenerateSummary(String productId) {
        summaryLoading.setValue(true);
        repository.getReviewSummary(productId, new ReviewRepository.StringCallback() {
            @Override
            public void onResult(@Nullable String cached) {
                if (cached != null && !cached.isEmpty()) {
                    summaryLoading.setValue(false);
                    aiSummary.setValue(cached);
                } else {
                    generateSummary(productId);
                }
            }

            @Override
            public void onError(Exception e) {
                generateSummary(productId);
            }
        });
    }

    private void generateSummary(String productId) {
        repository.getReviewsByProduct(productId, new ReviewRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> list) {
                if (list.isEmpty()) {
                    summaryLoading.setValue(false);
                    aiSummary.setValue("No reviews to summarize yet.");
                    return;
                }
                List<String> texts = new ArrayList<>();
                for (Review r : list) {
                    if (r.getContent() != null && !r.getContent().trim().isEmpty()) {
                        texts.add(r.getContent());
                    }
                }
                geminiService.summarizeReviews(texts, new GeminiService.GeminiCallback<String>() {
                    @Override
                    public void onSuccess(String summary) {
                        summaryLoading.setValue(false);
                        aiSummary.setValue(summary);
                        repository.saveReviewSummary(productId, summary,
                                new ReviewRepository.VoidCallback() {
                                    @Override
                                    public void onSuccess() {
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                    }
                                });
                    }

                    @Override
                    public void onError(Exception e) {
                        summaryLoading.setValue(false);
                        errorMessage.setValue(e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                summaryLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void assistReview(
            com.shoplens.ai.model.ReviewAssistAction action,
            String currentReviewText,
            float rating,
            com.shoplens.ai.model.Product product
    ) {
        isReviewAssistLoading.setValue(true);
        reviewAssistSuggestion.setValue(null);
        reviewAssistError.setValue(null);

        com.shoplens.ai.ai.ReviewAssistService assistService = new com.shoplens.ai.ai.ReviewAssistService(getApplication());
        assistService.assistReview(action, currentReviewText, rating, product, new com.shoplens.ai.ai.ReviewAssistService.ReviewAssistCallback() {
            @Override
            public void onSuccess(com.shoplens.ai.model.ReviewAssistResult result) {
                isReviewAssistLoading.setValue(false);
                reviewAssistSuggestion.setValue(result.getSuggestedText());
            }

            @Override
            public void onError(Exception exception) {
                isReviewAssistLoading.setValue(false);
                reviewAssistError.setValue(exception.getMessage());
            }
        });
    }
}
