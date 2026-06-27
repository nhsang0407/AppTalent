package com.shoplens.ai.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.ProductAdapter;
import com.shoplens.ai.databinding.ActivityProductManagementBinding;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProductManagementActivity extends AppCompatActivity
        implements ProductAdapter.OnProductClickListener {

    private ActivityProductManagementBinding binding;
    private ProductViewModel viewModel;
    private ProductAdapter adapter;

    private String categoryFilter = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        adapter = new ProductAdapter(true, true, this);
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProductActivity.class)));

        setupCategorySpinner();
        setupSearch();
        observeViewModel();
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add(getString(R.string.cat_all));
        categories.add("Fashion");
        categories.add("Electronics");
        categories.add("Food");
        categories.add("Home");
        categories.add("Beauty");
        categories.add("Other");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(spinnerAdapter);

        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoryFilter = position == 0 ? null : categories.get(position);
                viewModel.loadProducts(categoryFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchProducts(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null || newText.isEmpty()) {
                    viewModel.loadProducts(categoryFilter);
                }
                return true;
            }
        });
    }

    private void observeViewModel() {
        viewModel.getProducts().observe(this, products -> {
            adapter.submit(products);
            boolean empty = products == null || products.isEmpty();
            binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onProductClick(Product product) {
        openEdit(product);
    }

    @Override
    public void onEditClick(Product product) {
        openEdit(product);
    }

    @Override
    public void onDeleteClick(Product product) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_product_title)
                .setMessage(R.string.delete_product_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteProduct(product.getProductId());
                    Snackbar.make(binding.getRoot(), R.string.product_deleted,
                            Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private void openEdit(Product product) {
        Intent intent = new Intent(this, AddEditProductActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getProductId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadProducts(categoryFilter);
    }
}
