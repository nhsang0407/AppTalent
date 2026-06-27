package com.shoplens.ai.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.shoplens.ai.model.SearchLog;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes the "searchLogs" collection and aggregates trend analytics.
 */
public class SearchLogRepository {

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public interface SearchStatsCallback {
        void onSuccess(List<Map.Entry<String, Integer>> topLabels);

        void onError(Exception e);
    }

    public void logSearch(String userId, String searchType, String detectedLabel,
                          @NonNull VoidCallback callback) {
        SearchLog log = new SearchLog(userId, searchType, detectedLabel);
        DocumentReference ref =
                FirebaseUtils.getDb().collection(Constants.COLLECTION_SEARCH_LOGS).document();
        log.setLogId(ref.getId());
        ref.set(log)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Returns the most frequently detected labels, sorted by count descending, capped at {@code limit}.
     */
    public void getTopSearchLabels(int limit, @NonNull SearchStatsCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_SEARCH_LOGS).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    snapshot.forEach(doc -> {
                        String label = doc.getString("detectedLabel");
                        if (label == null || label.trim().isEmpty()) {
                            return;
                        }
                        String key = label.trim().toLowerCase(Locale.getDefault());
                        counts.put(key, counts.getOrDefault(key, 0) + 1);
                    });

                    List<Map.Entry<String, Integer>> entries = new ArrayList<>();
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        entries.add(new AbstractMap.SimpleEntry<>(
                                capitalize(e.getKey()), e.getValue()));
                    }
                    entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    if (entries.size() > limit) {
                        entries = new ArrayList<>(entries.subList(0, limit));
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onError);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
