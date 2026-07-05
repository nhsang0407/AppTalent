package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.adapter.AssistantMessageAdapter;
import com.shoplens.ai.databinding.ActivityShoppingAssistantBinding;
import com.shoplens.ai.viewmodel.ShoppingAssistantViewModel;

/**
 * Chat interface activity for the AI Shopping Assistant.
 */
public class ShoppingAssistantActivity extends AppCompatActivity {

    private ActivityShoppingAssistantBinding binding;
    private ShoppingAssistantViewModel viewModel;
    private AssistantMessageAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingAssistantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setupKeyboardInsets();

        viewModel = new ViewModelProvider(this).get(ShoppingAssistantViewModel.class);

        setupRecyclerView();
        setupInputListeners();

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new AssistantMessageAdapter(this::openDetail);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvAssistantMessages.setLayoutManager(layoutManager);
        binding.rvAssistantMessages.setAdapter(adapter);
    }

    private void setupKeyboardInsets() {
        View root = binding.rootShoppingAssistant;
        View imeSpacer = binding.viewImeSpacer;
        androidx.recyclerview.widget.RecyclerView recyclerView = binding.rvAssistantMessages;

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime());

            int spacerHeight = imeVisible ? ime.bottom : systemBars.bottom;

            android.view.ViewGroup.LayoutParams params = imeSpacer.getLayoutParams();
            if (params.height != spacerHeight) {
                params.height = spacerHeight;
                imeSpacer.setLayoutParams(params);
            }

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            if (adapter != null && adapter.getItemCount() > 0) {
                recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1), 150);
            }

            return insets;
        });

        androidx.core.view.ViewCompat.requestApplyInsets(root);
    }

    private void openDetail(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(com.shoplens.ai.utils.Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    private void setupInputListeners() {
        binding.btnSendQuestion.setOnClickListener(v -> sendMessage());

        binding.edtShoppingQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Automatically scroll to bottom when keyboard opens and edittext is focused
        binding.edtShoppingQuestion.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.rvAssistantMessages.postDelayed(() -> {
                    if (adapter.getItemCount() > 0) {
                        binding.rvAssistantMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });
    }

    private void sendMessage() {
        String text = binding.edtShoppingQuestion.getText() != null ?
                binding.edtShoppingQuestion.getText().toString().trim() : "";
        if (text.isEmpty()) {
            return;
        }

        viewModel.sendQuestion(text);
        binding.edtShoppingQuestion.setText("");
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(this, messages -> {
            adapter.submitList(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.rvAssistantMessages.post(() ->
                        binding.rvAssistantMessages.smoothScrollToPosition(messages.size() - 1));
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            binding.progressAssistant.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSendQuestion.setEnabled(!loading);
            binding.edtShoppingQuestion.setEnabled(!loading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.tvAssistantError.setText(error);
                binding.tvAssistantError.setVisibility(View.VISIBLE);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            } else {
                binding.tvAssistantError.setVisibility(View.GONE);
            }
        });
    }
}
