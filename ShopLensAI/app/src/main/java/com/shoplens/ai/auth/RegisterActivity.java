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
import com.shoplens.ai.databinding.ActivityRegisterBinding;
import com.shoplens.ai.user.HomeActivity;
import com.shoplens.ai.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeViewModel();

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.btnGoLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!loading);
        });

        viewModel.getUserRole().observe(this, role -> {
            if (role == null) {
                return;
            }
            // New users are always "user".
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void attemptRegister() {
        String name = textOf(binding.etName.getText());
        String email = textOf(binding.etEmail.getText());
        String password = textOf(binding.etPassword.getText());
        String confirm = textOf(binding.etConfirm.getText());

        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_field));
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirm.setError(getString(R.string.error_passwords_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }
        viewModel.register(email, password, name);
    }

    private static String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
