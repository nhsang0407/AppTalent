package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.OrderItemAdapter;
import com.shoplens.ai.databinding.ActivityCheckoutBinding;
import com.shoplens.ai.model.CartItem;
import com.shoplens.ai.model.OrderItem;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;
import com.shoplens.ai.viewmodel.CartViewModel;
import com.shoplens.ai.viewmodel.OrderViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private CartViewModel cartViewModel;
    private OrderViewModel orderViewModel;
    private OrderItemAdapter itemAdapter;

    private List<CartItem> cartItems = new ArrayList<>();
    private String userName = "Customer";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        itemAdapter = new OrderItemAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvItems.setAdapter(itemAdapter);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadUserProfile();
        bindSummary();
        observeViewModel();

        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void loadUserProfile() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            return;
        }
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String address = doc.getString("address");
                    if (name != null && !name.isEmpty()) {
                        userName = name;
                    }
                    if (address != null && !address.isEmpty()
                            && binding.etAddress.getText() != null
                            && binding.etAddress.getText().toString().isEmpty()) {
                        binding.etAddress.setText(address);
                    }
                });
    }

    private void bindSummary() {
        cartItems = cartViewModel.getCartItems().getValue();
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            orderItems.add(new OrderItem(
                    ci.getProduct().getProductId(),
                    ci.getProduct().getName(),
                    ci.getProduct().getDisplayImageUrl(),
                    ci.getProduct().getPrice(),
                    ci.getQuantity()));
        }
        itemAdapter.submit(orderItems);
        binding.tvTotal.setText(String.format(Locale.getDefault(),
                getString(R.string.price_format), cartViewModel.getTotalPrice()));
    }

    private void observeViewModel() {
        orderViewModel.getIsLoading().observe(this, loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnPlaceOrder.setEnabled(!loading);
        });

        orderViewModel.getOrderPlaced().observe(this, placed -> {
            if (Boolean.TRUE.equals(placed)) {
                cartViewModel.clearCart();
                showSuccessDialog();
            }
        });

        orderViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void placeOrder() {
        String address = binding.etAddress.getText() == null ? "" :
                binding.etAddress.getText().toString().trim();
        if (address.isEmpty()) {
            binding.tilAddress.setError(getString(R.string.error_empty_field));
            return;
        }
        binding.tilAddress.setError(null);
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            return;
        }
        orderViewModel.placeOrder(cartItems, uid, userName, address);
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_check_circle)
                .setTitle(R.string.order_placed_title)
                .setMessage(R.string.order_placed_sub)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(this, OrderHistoryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
