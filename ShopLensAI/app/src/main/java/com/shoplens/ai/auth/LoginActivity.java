package com.shoplens.ai.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.admin.AdminDashboardActivity;
import com.shoplens.ai.databinding.ActivityLoginBinding;
import com.shoplens.ai.user.HomeActivity;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.DatabaseSeeder;
import com.shoplens.ai.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        observeViewModel();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Seed products to Firestore if empty
        DatabaseSeeder.seedProductsIfNeeded();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
        });

        viewModel.getUserRole().observe(this, role -> {
            if (role == null) {
                return;
            }
            routeByRole(role);
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void routeByRole(String role) {
        Intent intent = Constants.ROLE_ADMIN.equals(role)
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void attemptLogin() {
        String email = textOf(binding.etEmail.getText());
        String password = textOf(binding.etPassword.getText());

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_field));
            valid = false;
        }
        if (!valid) {
            return;
        }
        viewModel.login(email, password);
    }

    private static String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
