package com.shoplens.ai.repository;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads and writes the "products" collection.
 */
public class ProductRepository {

    public interface ProductListCallback {
        void onSuccess(List<Product> products);

        void onError(Exception e);
    }

    public interface ProductCallback {
        void onSuccess(Product product);

        void onError(Exception e);
    }

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public void getAllProducts(@Nullable String categoryFilter,
                               @NonNull ProductListCallback callback) {
        Query query = FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS);
        if (categoryFilter != null && !categoryFilter.isEmpty()
                && !categoryFilter.equalsIgnoreCase("All")) {
            query = query.whereEqualTo("category", categoryFilter);
        }
        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        products.add(p);
                    });
                    callback.onSuccess(products);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getProductById(String productId, @NonNull ProductCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new IllegalStateException("Product not found."));
                        return;
                    }
                    Product p = doc.toObject(Product.class);
                    if (p != null) {
                        p.setProductId(doc.getId());
                    }
                    callback.onSuccess(p);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getProductByBarcode(String barcode, @NonNull ProductCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("barcode", barcode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onError(new IllegalStateException("No product for this barcode."));
                        return;
                    }
                    Product p = snapshot.getDocuments().get(0).toObject(Product.class);
                    if (p != null) {
                        p.setProductId(snapshot.getDocuments().get(0).getId());
                    }
                    callback.onSuccess(p);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Keyword search across name and tags. Firestore lacks full-text search, so we fetch and
     * filter client-side (adequate for a catalog of this scale).
     */
    public void searchProducts(String keyword, @NonNull ProductListCallback callback) {
        final String lower = keyword == null ? "" : keyword.toLowerCase(Locale.getDefault()).trim();
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> matches = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        if (matchesKeyword(p, lower)) {
                            matches.add(p);
                        }
                    });
                    callback.onSuccess(matches);
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean matchesKeyword(Product p, String lower) {
        if (lower.isEmpty()) {
            return true;
        }
        if (p.getName() != null
                && p.getName().toLowerCase(Locale.getDefault()).contains(lower)) {
            return true;
        }
        if (p.getCategory() != null
                && p.getCategory().toLowerCase(Locale.getDefault()).contains(lower)) {
            return true;
        }
        if (p.getTags() != null) {
            for (String tag : p.getTags()) {
                if (tag != null && tag.toLowerCase(Locale.getDefault()).contains(lower)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addProduct(Product product, @Nullable Uri imageUri,
                           @NonNull ProductCallback callback) {
        if (imageUri != null) {
            FirebaseUtils.uploadImageToStorage(imageUri, Constants.STORAGE_PRODUCTS,
                    new FirebaseUtils.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            product.setImageUrl(downloadUrl);
                            persistNewProduct(product, callback);
                        }

                        @Override
                        public void onError(Exception e) {
                            callback.onError(e);
                        }
                    });
        } else {
            persistNewProduct(product, callback);
        }
    }

    private void persistNewProduct(Product product, @NonNull ProductCallback callback) {
        DocumentReference ref =
                FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document();
        product.setProductId(ref.getId());
        ref.set(product)
                .addOnSuccessListener(unused -> callback.onSuccess(product))
                .addOnFailureListener(callback::onError);
    }

    /** Uploads a new image, then updates the existing product document with its URL. */
    public void updateProductWithImage(Product product, @NonNull Uri imageUri,
                                       @NonNull ProductCallback callback) {
        FirebaseUtils.uploadImageToStorage(imageUri, Constants.STORAGE_PRODUCTS,
                new FirebaseUtils.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        product.setImageUrl(downloadUrl);
                        updateProduct(product, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
    }

    public void updateProduct(Product product, @NonNull ProductCallback callback) {
        if (product.getProductId() == null) {
            callback.onError(new IllegalArgumentException("Missing product id."));
            return;
        }
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS)
                .document(product.getProductId())
                .set(product)
                .addOnSuccessListener(unused -> callback.onSuccess(product))
                .addOnFailureListener(callback::onError);
    }

    public void updateStock(String productId, int newStock, @NonNull VoidCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document(productId)
                .update("stock", newStock)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void deleteProduct(String productId, @NonNull VoidCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document(productId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /** Real-time products stream with optional category filter. */
    public LiveData<List<Product>> getProductsLiveData(@Nullable String category) {
        MutableLiveData<List<Product>> liveData = new MutableLiveData<>();
        Query query = FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS);
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("All")) {
            query = query.whereEqualTo("category", category);
        }
        query.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) {
                liveData.setValue(new ArrayList<>());
                return;
            }
            List<Product> products = new ArrayList<>();
            snapshot.forEach(doc -> {
                Product p = doc.toObject(Product.class);
                p.setProductId(doc.getId());
                products.add(p);
            });
            liveData.setValue(products);
        });
        return liveData;
    }
}
