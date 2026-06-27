package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.auth.LoginActivity;
import com.shoplens.ai.databinding.ActivityProfileBinding;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;
import com.shoplens.ai.viewmodel.AuthViewModel;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private AuthViewModel authViewModel;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        uid = FirebaseUtils.getCurrentUserId();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnLogout.setOnClickListener(v -> logout());

        loadUser();
    }

    private void loadUser() {
        if (uid == null) {
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    binding.progress.setVisibility(View.GONE);
                    binding.tvName.setText(doc.getString("name"));
                    binding.tvEmail.setText(doc.getString("email"));
                    binding.etPhone.setText(doc.getString("phone"));
                    binding.etAddress.setText(doc.getString("address"));
                })
                .addOnFailureListener(e -> {
                    binding.progress.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void saveChanges() {
        if (uid == null) {
            return;
        }
        String phone = binding.etPhone.getText() == null ? "" :
                binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText() == null ? "" :
                binding.etAddress.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("address", address);

        binding.progress.setVisibility(View.VISIBLE);
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    binding.progress.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), R.string.profile_updated,
                            Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progress.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void logout() {
        authViewModel.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
