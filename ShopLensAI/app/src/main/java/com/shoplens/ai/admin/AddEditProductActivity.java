package com.shoplens.ai.admin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ActivityAddEditProductBinding;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.ImageUtils;
import com.shoplens.ai.viewmodel.ProductViewModel;

import java.util.Arrays;
import java.util.List;

public class AddEditProductActivity extends AppCompatActivity {

    private ActivityAddEditProductBinding binding;
    private ProductViewModel viewModel;

    private final List<String> categories = Arrays.asList(
            "Fashion", "Electronics", "Food", "Home", "Beauty", "Other");

    private String editProductId;
    private Product editingProduct;
    private Uri selectedImageUri;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Uri> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    binding.ivProduct.setImageTintList(null);
                    Glide.with(this).load(selectedImageUri).into(binding.ivProduct);
                }
            });

    private final ActivityResultLauncher<Intent> scanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String barcode = result.getData().getStringExtra(Constants.EXTRA_BARCODE);
                            if (barcode != null) {
                                binding.etBarcode.setText(barcode);
                            }
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(spinnerAdapter);

        editProductId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        boolean editMode = editProductId != null;
        binding.toolbar.setTitle(editMode ? R.string.edit_product : R.string.add_product);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnTakePhoto.setOnClickListener(v -> takePhoto());
        binding.btnAiImage.setOnClickListener(v -> generateImage());
        binding.btnAiDescription.setOnClickListener(v -> generateDescription());
        binding.btnScan.setOnClickListener(v ->
                scanLauncher.launch(new Intent(this, com.shoplens.ai.user.BarcodeScanActivity.class)));
        binding.btnSave.setOnClickListener(v -> save());

        observeViewModel();

        if (editMode) {
            viewModel.getProductById(editProductId);
        }
    }

    private void observeViewModel() {
        viewModel.getSelectedProduct().observe(this, product -> {
            if (product != null && editProductId != null) {
                editingProduct = product;
                prefill(product);
            }
        });

        viewModel.getAiGeneratedImage().observe(this, bitmap -> {
            if (bitmap != null) {
                binding.ivProduct.setImageTintList(null);
                binding.ivProduct.setImageBitmap(bitmap);
                selectedImageUri = ImageUtils.bitmapToCacheUri(this, bitmap);
            }
        });

        viewModel.getAiGeneratedDescription().observe(this, desc -> {
            if (desc != null) {
                binding.etDescription.setText(desc);
            }
        });

        viewModel.getIsLoading().observe(this, loading ->
                binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getProductSaved().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), R.string.product_saved, Snackbar.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void prefill(Product p) {
        binding.etName.setText(p.getName());
        binding.etPrice.setText(String.valueOf(p.getPrice()));
        binding.etStock.setText(String.valueOf(p.getStock()));
        binding.etBarcode.setText(p.getBarcode());
        binding.etDescription.setText(p.getDescription());
        int catIndex = categories.indexOf(p.getCategory());
        if (catIndex >= 0) {
            binding.spinnerCategory.setSelection(catIndex);
        }
        if (p.getDisplayImageUrl() != null && !p.getDisplayImageUrl().isEmpty()) {
            binding.ivProduct.setImageTintList(null);
            Glide.with(this)
                    .load(p.getDisplayImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .into(binding.ivProduct);
        }
    }

    private void takePhoto() {
        cameraImageUri = ImageUtils.createCameraImageUri(this);
        if (cameraImageUri != null) {
            takePhotoLauncher.launch(cameraImageUri);
        } else {
            Snackbar.make(binding.getRoot(), R.string.something_went_wrong,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void generateImage() {
        String name = text(binding.etName);
        String desc = text(binding.etDescription);
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_field));
            return;
        }
        binding.tvLoading.setText(R.string.generating_image);
        viewModel.generateProductImage(name, desc);
    }

    private void generateDescription() {
        String name = text(binding.etName);
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_field));
            return;
        }
        String category = categories.get(binding.spinnerCategory.getSelectedItemPosition());
        binding.tvLoading.setText(R.string.generating_description);
        viewModel.generateProductDescription(name, category);
    }

    private void save() {
        String name = text(binding.etName);
        String priceStr = text(binding.etPrice);
        String stockStr = text(binding.etStock);
        String barcode = text(binding.etBarcode);
        String description = text(binding.etDescription);
        String category = categories.get(binding.spinnerCategory.getSelectedItemPosition());

        binding.tilName.setError(null);
        binding.tilPrice.setError(null);
        binding.tilStock.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_field));
            valid = false;
        }
        if (TextUtils.isEmpty(priceStr)) {
            binding.tilPrice.setError(getString(R.string.error_empty_field));
            valid = false;
        }
        if (TextUtils.isEmpty(stockStr)) {
            binding.tilStock.setError(getString(R.string.error_empty_field));
            valid = false;
        }
        if (!valid) {
            return;
        }

        double price = parseDouble(priceStr);
        int stock = parseInt(stockStr);

        binding.tvLoading.setText(R.string.loading);

        if (editProductId != null && editingProduct != null) {
            editingProduct.setName(name);
            editingProduct.setDescription(description);
            editingProduct.setCategory(category);
            editingProduct.setPrice(price);
            editingProduct.setStock(stock);
            editingProduct.setBarcode(barcode);
            if (selectedImageUri != null) {
                // New image chosen: upload then update.
                viewModel.addProductImageThenUpdate(editingProduct, selectedImageUri);
            } else {
                viewModel.updateProduct(editingProduct);
            }
        } else {
            Product product = new Product(name, description, category, price, stock);
            product.setBarcode(barcode);
            viewModel.addProduct(product, selectedImageUri);
        }
    }

    private static String text(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
