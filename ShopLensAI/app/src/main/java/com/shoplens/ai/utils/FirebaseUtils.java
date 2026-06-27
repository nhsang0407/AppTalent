package com.shoplens.ai.utils;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

/**
 * Thin helpers around Firebase singletons plus a couple of common async operations.
 */
public final class FirebaseUtils {

    private FirebaseUtils() {
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore getDb() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseStorage getStorage() {
        return FirebaseStorage.getInstance();
    }

    /** @return the current user's uid, or null if signed out. */
    public static String getCurrentUserId() {
        FirebaseUser user = getAuth().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getAuth().getCurrentUser() != null;
    }

    public interface RoleCallback {
        void onResult(String role);
    }

    public interface BooleanCallback {
        void onResult(boolean value);
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);

        void onError(Exception e);
    }

    /** Reads the current user's role from the users collection. Returns "user" on any failure. */
    public static void getCurrentUserRole(@NonNull RoleCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onResult(null);
            return;
        }
        getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    callback.onResult(role != null ? role : Constants.ROLE_USER);
                })
                .addOnFailureListener(e -> callback.onResult(Constants.ROLE_USER));
    }

    public static void isAdmin(@NonNull BooleanCallback callback) {
        getCurrentUserRole(role -> callback.onResult(Constants.ROLE_ADMIN.equals(role)));
    }

    /**
     * Uploads an image to Storage under the given folder path and returns its public download URL.
     */
    public static void uploadImageToStorage(@NonNull Uri imageUri, @NonNull String folderPath,
                                            @NonNull UploadCallback callback) {
        String fileName = folderPath + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = getStorage().getReference().child(fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Uploads raw JPEG bytes (used for AI-generated bitmaps) to Storage.
     */
    public static void uploadBytesToStorage(@NonNull byte[] data, @NonNull String folderPath,
                                            @NonNull UploadCallback callback) {
        String fileName = folderPath + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = getStorage().getReference().child(fileName);
        ref.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }
}
