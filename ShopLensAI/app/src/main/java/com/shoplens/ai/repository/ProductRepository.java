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
import com.shoplens.ai.api.CloudinaryApiService;
import com.shoplens.ai.api.CloudinaryClient;
import com.shoplens.ai.ShopLensApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Response;

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

    private interface CloudinaryUploadCallback {
        void onSuccess(String secureUrl);
        void onError(Exception e);
    }

    private void uploadImageToCloudinary(@NonNull Uri imageUri, @NonNull CloudinaryUploadCallback callback) {
        String TAG = "ProductRepository";
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        new Thread(() -> {
            try {
                android.util.Log.d(TAG, "uploadImageToCloudinary: start uri=" + imageUri);
                // Read & compress bitmap
                android.graphics.Bitmap bitmap = com.shoplens.ai.utils.ImageUtils
                        .uriToBitmap(com.shoplens.ai.ShopLensApplication.getInstance(), imageUri);
                if (bitmap == null) {
                    android.util.Log.e(TAG, "uploadImageToCloudinary: failed to read bitmap");
                    mainHandler.post(() -> callback.onError(new Exception("Không thể đọc ảnh sản phẩm.")));
                    return;
                }

                // Scale down to max 1024px side
                bitmap = com.shoplens.ai.utils.ImageUtils.scaleDown(bitmap, 1024);
                byte[] bytes = com.shoplens.ai.utils.ImageUtils.bitmapToJpegBytes(bitmap, 85);

                // Save to cache file for logging path & size
                File cacheDir = com.shoplens.ai.ShopLensApplication.getInstance().getCacheDir();
                File tempFile = new File(cacheDir, "product_compressed_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(bytes);
                }

                android.util.Log.d(TAG, "uploadImageToCloudinary: compressed product image path=" + tempFile.getAbsolutePath());
                android.util.Log.d(TAG, "uploadImageToCloudinary: compressed product image size=" + tempFile.length() + " bytes");

                // Build multipart payload
                RequestBody presetBody = RequestBody.create(
                        MediaType.parse("text/plain"),
                        "yxmkshhb"
                );
                RequestBody fileBody = RequestBody.create(
                        MediaType.parse("image/jpeg"),
                        tempFile
                );
                MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                        "file",
                        tempFile.getName(),
                        fileBody
                );

                // API call
                CloudinaryApiService service = CloudinaryClient.getApiService();
                Call<ResponseBody> call = service.uploadImage("pmvkdo8v", presetBody, filePart);

                android.util.Log.d(TAG, "uploadImageToCloudinary: Cloudinary product upload start");
                Response<ResponseBody> response = call.execute();
                int code = response.code();
                android.util.Log.d(TAG, "uploadImageToCloudinary: Cloudinary response code=" + code);

                if (!response.isSuccessful()) {
                    String error = response.errorBody() != null ? response.errorBody().string() : "";
                    android.util.Log.e(TAG, "uploadImageToCloudinary: Cloudinary upload failed, body=" + error);
                    mainHandler.post(() -> callback.onError(new Exception("Không thể tải ảnh sản phẩm lên. Vui lòng thử lại.")));
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                JSONObject json = new JSONObject(body);
                String secureUrl = json.optString("secure_url");
                android.util.Log.d(TAG, "uploadImageToCloudinary: Cloudinary secure_url=" + secureUrl);

                if (secureUrl == null || secureUrl.isEmpty() || (!secureUrl.startsWith("http://") && !secureUrl.startsWith("https://"))) {
                    android.util.Log.e(TAG, "uploadImageToCloudinary: invalid secure_url=" + secureUrl);
                    mainHandler.post(() -> callback.onError(new Exception("Không thể tải ảnh sản phẩm lên. Vui lòng thử lại.")));
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(secureUrl));
            } catch (Exception e) {
                android.util.Log.e(TAG, "uploadImageToCloudinary: exception", e);
                mainHandler.post(() -> callback.onError(e));
            }
        }).start();
    }

    public void addProduct(Product product, @Nullable Uri imageUri,
                           @NonNull ProductCallback callback) {
        android.util.Log.d("ProductRepository", "addProduct: start, product=" + product.getName() + ", imageUri=" + imageUri);
        if (imageUri != null) {
            uploadImageToCloudinary(imageUri, new CloudinaryUploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    product.setImageUrl(secureUrl);
                    android.util.Log.d("ProductRepository", "addProduct: Cloudinary upload success. imageUrl before save=" + product.getImageUrl());
                    persistNewProduct(product, callback);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            android.util.Log.d("ProductRepository", "addProduct: no imageUri provided. imageUrl before save=" + product.getImageUrl());
            persistNewProduct(product, callback);
        }
    }

    private void persistNewProduct(Product product, @NonNull ProductCallback callback) {
        DocumentReference ref =
                FirebaseUtils.getDb().collection(Constants.COLLECTION_PRODUCTS).document();
        product.setProductId(ref.getId());
        ref.set(product)
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("ProductRepository", "persistNewProduct: Firestore product save success. id=" + product.getProductId());
                    callback.onSuccess(product);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProductRepository", "persistNewProduct: Firestore product save fail", e);
                    callback.onError(e);
                });
    }

    /** Uploads a new image, then updates the existing product document with its URL. */
    public void updateProductWithImage(Product product, @NonNull Uri imageUri,
                                       @NonNull ProductCallback callback) {
        android.util.Log.d("ProductRepository", "updateProductWithImage: start, product=" + product.getName() + ", imageUri=" + imageUri);
        uploadImageToCloudinary(imageUri, new CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String secureUrl) {
                product.setImageUrl(secureUrl);
                android.util.Log.d("ProductRepository", "updateProductWithImage: Cloudinary upload success. imageUrl before save=" + product.getImageUrl());
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
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("ProductRepository", "updateProduct: Firestore product save success. id=" + product.getProductId());
                    callback.onSuccess(product);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProductRepository", "updateProduct: Firestore product save fail", e);
                    callback.onError(e);
                });
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
