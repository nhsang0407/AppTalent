package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.badge.BadgeDrawable;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.ProductAdapter;
import com.shoplens.ai.databinding.ActivityHomeBinding;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.DatabaseSeeder;
import com.shoplens.ai.viewmodel.CartViewModel;
import com.shoplens.ai.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private ProductViewModel productViewModel;
    private CartViewModel cartViewModel;
    private ProductAdapter adapter;

    private String currentCategory = null;
    private boolean pendingBarcodeLookup = false;
    private List<Product> originalProductsList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> visualSearchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Constants.RESULT_BARCODE
                                && result.getData() != null) {
                            String barcode = result.getData().getStringExtra(Constants.EXTRA_BARCODE);
                            handleBarcodeResult(barcode);
                        } else if (result.getResultCode() == Constants.RESULT_VISUAL
                                && result.getData() != null) {
                            String query = result.getData().getStringExtra(Constants.EXTRA_SEARCH_QUERY);
                            if (query != null) {
                                productViewModel.searchProducts(query);
                            }
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        setupRecycler();
        setupChips();
        setupBottomNav();
        observeViewModel();

        binding.fabCamera.setOnClickListener(v ->
                visualSearchLauncher.launch(new Intent(this, VisualSearchActivity.class)));
        binding.swipeRefresh.setOnRefreshListener(() -> productViewModel.loadProducts(currentCategory));
        binding.btnApplyFilter.setOnClickListener(v -> applyPriceFilter());

        productViewModel.loadProducts(null);

        // Seed products to Firestore if empty
        DatabaseSeeder.seedProductsIfNeeded();
    }

    private void setupRecycler() {
        adapter = new ProductAdapter(false, product ->
                openDetail(product.getProductId()));
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(adapter);
    }

    private void setupChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_all) {
                currentCategory = null;
            } else if (id == R.id.chip_fashion) {
                currentCategory = "Fashion";
            } else if (id == R.id.chip_electronics) {
                currentCategory = "Electronics";
            } else if (id == R.id.chip_food) {
                currentCategory = "Food";
            } else if (id == R.id.chip_home) {
                currentCategory = "Home";
            } else if (id == R.id.chip_beauty) {
                currentCategory = "Beauty";
            }
            productViewModel.loadProducts(currentCategory);
        });
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_search) {
                visualSearchLauncher.launch(new Intent(this, VisualSearchActivity.class));
                return false;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return false;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
                return false;
            }
            return false;
        });
    }

    private void observeViewModel() {
        productViewModel.getProducts().observe(this, products -> {
            originalProductsList = products != null ? products : new ArrayList<>();
            applyPriceFilter();
        });

        productViewModel.getIsLoading().observe(this, loading -> {
            binding.swipeRefresh.setRefreshing(false);
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        productViewModel.getSelectedProduct().observe(this, product -> {
            if (pendingBarcodeLookup && product != null) {
                pendingBarcodeLookup = false;
                openDetail(product.getProductId());
            }
        });

        productViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                com.google.android.material.snackbar.Snackbar
                        .make(binding.getRoot(), msg, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void handleBarcodeResult(String barcode) {
        if (barcode == null) {
            return;
        }
        pendingBarcodeLookup = true;
        productViewModel.getProductByBarcode(barcode);
    }

    private void openDetail(String productId) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void updateCartBadge() {
        int count = cartViewModel.getTotalItemCount();
        BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.nav_cart);
        badge.setVisible(count > 0);
        badge.setNumber(count);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    productViewModel.searchProducts(query);
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText == null || newText.isEmpty()) {
                        productViewModel.loadProducts(currentCategory);
                    }
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyPriceFilter() {
        if (originalProductsList == null) {
            return;
        }

        String minStr = binding.etMinPrice.getText() != null ?
                binding.etMinPrice.getText().toString().trim() : "";
        String maxStr = binding.etMaxPrice.getText() != null ?
                binding.etMaxPrice.getText().toString().trim() : "";

        double minPrice = 0.0;
        double maxPrice = Double.MAX_VALUE;

        try {
            if (!minStr.isEmpty()) {
                minPrice = Double.parseDouble(minStr);
            }
        } catch (NumberFormatException e) {
            // Ignore
        }

        try {
            if (!maxStr.isEmpty()) {
                maxPrice = Double.parseDouble(maxStr);
            }
        } catch (NumberFormatException e) {
            // Ignore
        }

        if (minPrice > maxPrice) {
            binding.tilMinPrice.setError("Min > Max");
            return;
        } else {
            binding.tilMinPrice.setError(null);
        }

        List<Product> filteredList = new ArrayList<>();
        for (Product p : originalProductsList) {
            if (p.getPrice() >= minPrice && p.getPrice() <= maxPrice) {
                filteredList.add(p);
            }
        }

        adapter.submit(filteredList);
        boolean empty = filteredList.isEmpty();
        binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
