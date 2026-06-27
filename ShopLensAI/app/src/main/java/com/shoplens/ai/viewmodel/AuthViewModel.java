package com.shoplens.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.shoplens.ai.repository.AuthRepository;
import com.shoplens.ai.utils.FirebaseUtils;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository = new AuthRepository();

    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<String> getUserRole() {
        return userRole;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<FirebaseUser> getCurrentUser() {
        return currentUser;
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        repository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String role) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                userRole.setValue(role);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void register(String email, String password, String name) {
        isLoading.setValue(true);
        repository.register(email, password, name, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String role) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                userRole.setValue(role);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void logout() {
        repository.logout();
        currentUser.setValue(null);
        userRole.setValue(null);
    }

    /** Called on app start to redirect already-authenticated users. */
    public void checkCurrentSession() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            userRole.setValue(null);
            return;
        }
        isLoading.setValue(true);
        repository.getUserRole(uid, new AuthRepository.RoleCallback() {
            @Override
            public void onResult(String role) {
                isLoading.setValue(false);
                currentUser.setValue(FirebaseUtils.getAuth().getCurrentUser());
                userRole.setValue(role);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
