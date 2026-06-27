package com.shoplens.ai.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.shoplens.ai.model.Review;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the "reviews" collection and keeps product rating aggregates in sync.
 */
public class ReviewRepository {

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);

        void onError(Exception e);
    }

    public interface StringCallback {
        /** value is null when there is no cached summary. */
        void onResult(@Nullable String value);

        void onError(Exception e);
    }

    /**
     * Adds a review and atomically updates the product's averageRating / reviewCount
     * inside a Firestore transaction.
     */
    public void addReview(Review review, @NonNull VoidCallback callback) {
        DocumentReference reviewRef =
                FirebaseUtils.getDb().collection(Constants.COLLECTION_REVIEWS).document();
        review.setReviewId(reviewRef.getId());
        DocumentReference productRef = FirebaseUtils.getDb()
                .collection(Constants.COLLECTION_PRODUCTS).document(review.getProductId());

        FirebaseUtils.getDb().runTransaction(transaction -> {
            DocumentSnapshot productSnap = transaction.get(productRef);
            double currentAvg = productSnap.getDouble("averageRating") != null
                    ? productSnap.getDouble("averageRating") : 0.0;
            long currentCount = productSnap.getLong("reviewCount") != null
                    ? productSnap.getLong("reviewCount") : 0L;

            double newCount = currentCount + 1;
            double newAvg = ((currentAvg * currentCount) + review.getRating()) / newCount;

            transaction.set(reviewRef, review);
            transaction.update(productRef, "averageRating", newAvg);
            transaction.update(productRef, "reviewCount", (int) newCount);
            // Invalidate cached AI summary since reviews changed.
            transaction.update(productRef, "reviewSummary", null);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getReviewsByProduct(String productId, @NonNull ReviewListCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Review r = doc.toObject(Review.class);
                        r.setReviewId(doc.getId());
                        reviews.add(r);
                    });
                    reviews.sort((a, b) -> {
                        long ta = a.getCreatedAt() != null ? a.getCreatedAt().toDate().getTime() : 0;
                        long tb = b.getCreatedAt() != null ? b.getCreatedAt().toDate().getTime() : 0;
                        return Long.compare(tb, ta);
                    });
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(callback::onError);
    }

    /** Returns the cached AI summary stored on the product document, or null if absent. */
    public void getReviewSummary(String productId, @NonNull StringCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document(productId).get()
                .addOnSuccessListener(doc -> callback.onResult(doc.getString("reviewSummary")))
                .addOnFailureListener(callback::onError);
    }

    public void saveReviewSummary(String productId, String summary, @NonNull VoidCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document(productId)
                .update("reviewSummary", summary)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}
