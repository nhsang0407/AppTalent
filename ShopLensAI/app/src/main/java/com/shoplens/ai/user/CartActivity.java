package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.CartAdapter;
import com.shoplens.ai.databinding.ActivityCartBinding;
import com.shoplens.ai.model.CartItem;
import com.shoplens.ai.viewmodel.CartViewModel;

import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartListener {

    private ActivityCartBinding binding;
    private CartViewModel cartViewModel;
    private CartAdapter adapter;
    private List<CartItem> currentItems;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        adapter = new CartAdapter(this);
        binding.rvCart.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCart.setAdapter(adapter);
        attachSwipeToDelete();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnCheckout.setOnClickListener(v -> {
            if (currentItems == null || currentItems.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.empty_cart_title, Snackbar.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, CheckoutActivity.class));
        });

        observeViewModel();
    }

    private void observeViewModel() {
        cartViewModel.getCartItems().observe(this, items -> {
            currentItems = items;
            adapter.submit(items);
            boolean empty = items == null || items.isEmpty();
            binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvCart.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.summaryBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        cartViewModel.getTotalPriceLiveData().observe(this, total ->
                binding.tvSubtotal.setText(String.format(Locale.getDefault(),
                        getString(R.string.price_format), total == null ? 0.0 : total)));
    }

    private void attachSwipeToDelete() {
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        int pos = vh.getBindingAdapterPosition();
                        if (currentItems != null && pos >= 0 && pos < currentItems.size()) {
                            onRemove(currentItems.get(pos).getProduct().getProductId());
                        }
                    }
                });
        helper.attachToRecyclerView(binding.rvCart);
    }

    @Override
    public void onQuantityChange(String productId, int newQuantity) {
        cartViewModel.updateQuantity(productId, newQuantity);
    }

    @Override
    public void onRemove(String productId) {
        cartViewModel.removeFromCart(productId);
    }
}
