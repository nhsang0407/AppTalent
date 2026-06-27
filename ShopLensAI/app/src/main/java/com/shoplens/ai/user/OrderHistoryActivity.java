package com.shoplens.ai.user;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.adapter.OrderAdapter;
import com.shoplens.ai.databinding.ActivityOrderHistoryBinding;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.utils.FirebaseUtils;
import com.shoplens.ai.viewmodel.OrderViewModel;

public class OrderHistoryActivity extends AppCompatActivity
        implements OrderAdapter.OnOrderActionListener {

    private ActivityOrderHistoryBinding binding;
    private OrderViewModel orderViewModel;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        adapter = new OrderAdapter(false, this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        observeViewModel();

        String uid = FirebaseUtils.getCurrentUserId();
        if (uid != null) {
            orderViewModel.loadUserOrders(uid);
        }
    }

    private void observeViewModel() {
        orderViewModel.getOrders().observe(this, orders -> {
            adapter.submit(orders);
            boolean empty = orders == null || orders.isEmpty();
            binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        orderViewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        orderViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStatusChange(Order order, String newStatus) {
        // No-op for users (read-only).
    }

    @Override
    public void onOrderClick(Order order) {
        // Expansion handled in the adapter.
    }
}
