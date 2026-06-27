package com.shoplens.ai.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.shoplens.ai.model.User;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;

/**
 * Handles authentication and the linked Firestore user document.
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(FirebaseUser user, String role);

        void onError(Exception e);
    }

    public interface RoleCallback {
        void onResult(String role);

        void onError(Exception e);
    }

    public void login(String email, String password, @NonNull AuthCallback callback) {
        FirebaseUtils.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError(new IllegalStateException("Login failed."));
                        return;
                    }
                    getUserRole(user.getUid(), new RoleCallback() {
                        @Override
                        public void onResult(String role) {
                            callback.onSuccess(user, role);
                        }

                        @Override
                        public void onError(Exception e) {
                            // Default any role-read failure to "user" so login still succeeds.
                            callback.onSuccess(user, Constants.ROLE_USER);
                        }
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    public void register(String email, String password, String name,
                         @NonNull AuthCallback callback) {
        FirebaseUtils.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener((AuthResult authResult) -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError(new IllegalStateException("Registration failed."));
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    User user = new User(uid, name, email, Constants.ROLE_USER);
                    FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS)
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(firebaseUser, Constants.ROLE_USER))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getUserRole(String uid, @NonNull RoleCallback callback) {
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    callback.onResult(role != null ? role : Constants.ROLE_USER);
                })
                .addOnFailureListener(callback::onError);
    }

    public void logout() {
        FirebaseUtils.getAuth().signOut();
    }
}
