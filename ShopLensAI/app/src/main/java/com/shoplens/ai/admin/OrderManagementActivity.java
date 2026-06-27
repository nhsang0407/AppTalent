package com.shoplens.ai.admin;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.OrderAdapter;
import com.shoplens.ai.databinding.ActivityOrderManagementBinding;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.viewmodel.OrderViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderManagementActivity extends AppCompatActivity
        implements OrderAdapter.OnOrderActionListener {

    private ActivityOrderManagementBinding binding;
    private OrderViewModel viewModel;
    private OrderAdapter adapter;

    private List<Order> allOrders = new ArrayList<>();
    private String currentFilter = null; // null = All

    private static final String[] TAB_FILTERS = {
            null,
            Constants.STATUS_PENDING,
            Constants.STATUS_CONFIRMED,
            Constants.STATUS_DONE,
            Constants.STATUS_CANCELLED
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        adapter = new OrderAdapter(true, this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setupTabs();
        observeViewModel();

        viewModel.loadAllOrders();
    }

    private void setupTabs() {
        int[] labels = {R.string.tab_all, R.string.status_pending, R.string.status_confirmed,
                R.string.status_done, R.string.status_cancelled};
        for (int label : labels) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(label));
        }
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = TAB_FILTERS[tab.getPosition()];
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void observeViewModel() {
        viewModel.getOrders().observe(this, orders -> {
            allOrders = orders != null ? orders : new ArrayList<>();
            applyFilter();
        });

        viewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            if (currentFilter == null || currentFilter.equals(o.getStatus())) {
                filtered.add(o);
            }
        }
        adapter.submit(filtered);
        boolean empty = filtered.isEmpty();
        binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStatusChange(Order order, String newStatus) {
        if (Constants.STATUS_CANCELLED.equals(newStatus)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.status_cancelled)
                    .setMessage(R.string.confirm_cancel_order)
                    .setNegativeButton(R.string.cancel, (d, w) -> viewModel.loadAllOrders())
                    .setPositiveButton(R.string.confirm, (d, w) ->
                            viewModel.updateOrderStatus(order.getOrderId(), newStatus))
                    .show();
        } else {
            viewModel.updateOrderStatus(order.getOrderId(), newStatus);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        // Expansion handled in adapter.
    }
}
