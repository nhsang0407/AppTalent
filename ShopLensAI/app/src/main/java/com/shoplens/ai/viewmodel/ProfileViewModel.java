package com.shoplens.ai.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.shoplens.ai.model.Address;
import com.shoplens.ai.model.User;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for ProfileActivity, AddressListActivity, AddressFormActivity.
 * Handles: load user, update personal info, avatar upload/remove,
 * address CRUD, and language switching.
 */
public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<List<Address>> addresses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> avatarUploading = new MutableLiveData<>(false);

    public ProfileViewModel(@NonNull Application application) {
        super(application);
    }

    // --- Getters ---

    public MutableLiveData<User> getUser() { return user; }
    public MutableLiveData<List<Address>> getAddresses() { return addresses; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
    public MutableLiveData<String> getSuccessMessage() { return successMessage; }
    public MutableLiveData<Boolean> getAvatarUploading() { return avatarUploading; }

    // --- User ---

    public void loadUser() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;
        isLoading.setValue(true);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    isLoading.setValue(false);
                    User u = doc.toObject(User.class);
                    if (u != null) u.setUid(doc.getId());
                    user.setValue(u);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void updatePersonalInfo(String name, String phone) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        isLoading.setValue(true);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    // Update local user object
                    User current = user.getValue();
                    if (current != null) {
                        current.setName(name);
                        current.setPhone(phone);
                        user.setValue(current);
                    }
                    successMessage.setValue("profile_updated");
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    // --- Avatar ---

    public void updateAvatar(@NonNull Uri imageUri) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            errorMessage.setValue("Vui lòng đăng nhập lại.");
            return;
        }
        avatarUploading.setValue(true);
        android.util.Log.d(TAG, "updateAvatar: start, uri=" + imageUri);

        // Read & compress bitmap on background thread to avoid content URI permission issues
        // that cause putFile() to fail silently with "Object does not exist at location"
        new Thread(() -> {
            try {
                android.graphics.Bitmap bitmap = com.shoplens.ai.utils.ImageUtils
                        .uriToBitmap(getApplication(), imageUri);
                if (bitmap == null) {
                    android.util.Log.e(TAG, "updateAvatar: failed to read bitmap from URI");
                    avatarUploading.postValue(false);
                    errorMessage.postValue("Không thể đọc ảnh. Vui lòng thử ảnh khác.");
                    return;
                }
                // Scale down to max 1024px side
                bitmap = com.shoplens.ai.utils.ImageUtils.scaleDown(bitmap, 1024);
                byte[] bytes = com.shoplens.ai.utils.ImageUtils.bitmapToJpegBytes(bitmap, 85);
                android.util.Log.d(TAG, "updateAvatar: compressed " + bytes.length + " bytes");

                // Use uid-based filename so each user has one avatar slot
                String storagePath = Constants.STORAGE_AVATARS + uid + "_"
                        + System.currentTimeMillis() + ".jpg";
                android.util.Log.d(TAG, "updateAvatar: uploading to path=" + storagePath);

                // Build storage ref and upload bytes directly (avoids content:// URI issues)
                com.google.firebase.storage.StorageReference ref = FirebaseUtils.getStorage()
                        .getReference().child(storagePath);
                ref.putBytes(bytes)
                        .addOnSuccessListener(taskSnapshot -> {
                            android.util.Log.d(TAG, "updateAvatar: putBytes success, getting downloadUrl");
                            ref.getDownloadUrl()
                                    .addOnSuccessListener(dlUri -> {
                                        String downloadUrl = dlUri.toString();
                                        android.util.Log.d(TAG, "updateAvatar: downloadUrl=" + downloadUrl);
                                        // Save to Firestore
                                        FirebaseUtils.getDb()
                                                .collection(Constants.COLLECTION_USERS).document(uid)
                                                .update("avatarUrl", downloadUrl)
                                                .addOnSuccessListener(unused -> {
                                                    android.util.Log.d(TAG, "updateAvatar: Firestore saved");
                                                    avatarUploading.setValue(false);
                                                    User current = user.getValue();
                                                    if (current != null) {
                                                        current.setAvatarUrl(downloadUrl);
                                                        user.setValue(current);
                                                    }
                                                    successMessage.setValue("avatar_updated");
                                                })
                                                .addOnFailureListener(e -> {
                                                    android.util.Log.e(TAG, "updateAvatar: Firestore update failed", e);
                                                    avatarUploading.setValue(false);
                                                    errorMessage.setValue("Không thể lưu ảnh đại diện. Vui lòng thử lại.");
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e(TAG, "updateAvatar: getDownloadUrl failed", e);
                                        avatarUploading.setValue(false);
                                        errorMessage.setValue("Không thể cập nhật ảnh đại diện. Vui lòng thử lại.");
                                    });
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e(TAG, "updateAvatar: putBytes failed", e);
                            avatarUploading.setValue(false);
                            errorMessage.setValue("Không thể tải ảnh lên. Vui lòng kiểm tra kết nối và thử lại.");
                        });
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateAvatar: exception", e);
                avatarUploading.postValue(false);
                errorMessage.postValue("Không thể cập nhật ảnh đại diện. Vui lòng thử lại.");
            }
        }).start();
    }

    public void removeAvatar() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;
        isLoading.setValue(true);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid)
                .update("avatarUrl", null)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    User current = user.getValue();
                    if (current != null) {
                        current.setAvatarUrl(null);
                        user.setValue(current);
                    }
                    successMessage.setValue("avatar_removed");
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    // --- Addresses ---

    public void loadAddresses() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid)
                .collection(Constants.COLLECTION_ADDRESSES)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        errorMessage.setValue(e.getMessage());
                        return;
                    }
                    if (snapshots == null) return;
                    List<Address> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        Address addr = doc.toObject(Address.class);
                        if (addr != null) {
                            addr.setId(doc.getId());
                            list.add(addr);
                        }
                    }
                    // Sort: default first, then by label
                    list.sort((a, b) -> {
                        if (a.isDefault() && !b.isDefault()) return -1;
                        if (!a.isDefault() && b.isDefault()) return 1;
                        return 0;
                    });
                    addresses.setValue(list);
                });
    }

    public void addAddress(@NonNull Address address) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;
        isLoading.setValue(true);

        List<Address> current = addresses.getValue();
        boolean isFirst = current == null || current.isEmpty();
        if (isFirst) address.setDefault(true);

        DocumentReference newRef = FirebaseUtils.getDb()
                .collection(Constants.COLLECTION_USERS).document(uid)
                .collection(Constants.COLLECTION_ADDRESSES).document();

        Map<String, Object> data = addressToMap(address);
        data.put("id", newRef.getId());

        WriteBatch batch = FirebaseUtils.getDb().batch();
        batch.set(newRef, data);

        // If new address is default, clear old default
        if (address.isDefault() && current != null) {
            for (Address a : current) {
                if (a.isDefault() && !a.getId().equals(newRef.getId())) {
                    DocumentReference oldRef = FirebaseUtils.getDb()
                            .collection(Constants.COLLECTION_USERS).document(uid)
                            .collection(Constants.COLLECTION_ADDRESSES).document(a.getId());
                    batch.update(oldRef, "isDefault", false);
                }
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    successMessage.setValue("address_saved");
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void updateAddress(@NonNull Address address) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null || address.getId() == null) return;
        isLoading.setValue(true);

        WriteBatch batch = FirebaseUtils.getDb().batch();

        DocumentReference thisRef = FirebaseUtils.getDb()
                .collection(Constants.COLLECTION_USERS).document(uid)
                .collection(Constants.COLLECTION_ADDRESSES).document(address.getId());
        batch.set(thisRef, addressToMap(address));

        // If setting as default, clear old defaults
        if (address.isDefault()) {
            List<Address> current = addresses.getValue();
            if (current != null) {
                for (Address a : current) {
                    if (a.isDefault() && !a.getId().equals(address.getId())) {
                        DocumentReference oldRef = FirebaseUtils.getDb()
                                .collection(Constants.COLLECTION_USERS).document(uid)
                                .collection(Constants.COLLECTION_ADDRESSES).document(a.getId());
                        batch.update(oldRef, "isDefault", false);
                    }
                }
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    successMessage.setValue("address_saved");
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void deleteAddress(@NonNull String addressId) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        List<Address> current = addresses.getValue();
        boolean wasDefault = false;
        if (current != null) {
            for (Address a : current) {
                if (a.getId().equals(addressId)) {
                    wasDefault = a.isDefault();
                    break;
                }
            }
        }

        final boolean finalWasDefault = wasDefault;
        isLoading.setValue(true);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid)
                .collection(Constants.COLLECTION_ADDRESSES).document(addressId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    successMessage.setValue("address_deleted");
                    // Auto-assign new default if deleted was default
                    if (finalWasDefault && current != null) {
                        for (Address a : current) {
                            if (!a.getId().equals(addressId)) {
                                setDefaultAddress(a.getId());
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void setDefaultAddress(@NonNull String addressId) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        List<Address> current = addresses.getValue();
        if (current == null) return;

        WriteBatch batch = FirebaseUtils.getDb().batch();
        for (Address a : current) {
            DocumentReference ref = FirebaseUtils.getDb()
                    .collection(Constants.COLLECTION_USERS).document(uid)
                    .collection(Constants.COLLECTION_ADDRESSES).document(a.getId());
            batch.update(ref, "isDefault", a.getId().equals(addressId));
        }
        batch.commit()
                .addOnSuccessListener(unused -> successMessage.setValue("default_assigned"))
                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
    }

    // --- Language ---

    public void setLanguage(@NonNull String langCode) {
        // Persist
        getApplication().getSharedPreferences(Constants.PREF_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putString(Constants.PREF_LANGUAGE, langCode).apply();
        // Apply immediately via AndroidX LocaleManager (API 33+), fallback handled in Activity
        LocaleListCompat localeList = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(localeList);
    }

    public String getSavedLanguage() {
        return getApplication().getSharedPreferences(Constants.PREF_NAME, android.content.Context.MODE_PRIVATE)
                .getString(Constants.PREF_LANGUAGE, Constants.LANG_EN);
    }

    // --- Helpers ---

    private Map<String, Object> addressToMap(Address address) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", address.getId());
        map.put("receiverName", address.getReceiverName());
        map.put("phone", address.getPhone());
        map.put("detail", address.getDetail());
        map.put("ward", address.getWard() != null ? address.getWard() : "");
        map.put("district", address.getDistrict() != null ? address.getDistrict() : "");
        map.put("city", address.getCity() != null ? address.getCity() : "");
        map.put("label", address.getLabel() != null ? address.getLabel() : "Other");
        map.put("isDefault", address.isDefault());
        return map;
    }
}
