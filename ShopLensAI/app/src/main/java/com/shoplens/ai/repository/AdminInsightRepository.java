package com.shoplens.ai.repository;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository to manage fetching and caching Admin Insights in Firestore.
 */
public class AdminInsightRepository {

    public interface InsightCallback {
        void onSuccess(String content);

        void onError(Exception e);
    }

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception e);
    }

    /**
     * Fetches the cached admin insight for a specific date (yyyy-MM-dd).
     */
    public void getAdminInsight(@NonNull String dateKey, @NonNull InsightCallback callback) {
        FirebaseUtils.getDb().collection("adminInsights").document(dateKey).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String content = documentSnapshot.getString("content");
                        callback.onSuccess(content);
                    } else {
                        callback.onSuccess(null); // No cache found
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Saves the admin insight content for a specific date (yyyy-MM-dd) to Firestore.
     */
    public void saveAdminInsight(@NonNull String dateKey, @NonNull String content, VoidCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("createdAt", Timestamp.now());
        data.put("dateKey", dateKey);
        data.put("model", "gemini-2.5-flash");

        FirebaseUtils.getDb().collection("adminInsights").document(dateKey).set(data)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }
}
