package com.shoplens.ai.admin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.shoplens.ai.R;
import com.shoplens.ai.ai.MLKitService;
import com.shoplens.ai.databinding.ActivityBarcodeStockBinding;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.viewmodel.ProductViewModel;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeStockActivity extends AppCompatActivity {

    private ActivityBarcodeStockBinding binding;
    private ProductViewModel viewModel;
    private ExecutorService cameraExecutor;

    private volatile boolean scanningPaused = false;
    private Product currentProduct;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnScanAnother.setOnClickListener(v -> resetForNextScan());
        binding.btnUpdateStock.setOnClickListener(v -> updateStock());

        observeViewModel();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void observeViewModel() {
        viewModel.getSelectedProduct().observe(this, product -> {
            if (product != null && scanningPaused) {
                currentProduct = product;
                showPanel(product);
            }
        });

        viewModel.getProductSaved().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), R.string.stock_updated,
                        Snackbar.LENGTH_SHORT).show();
                resetForNextScan();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
                resetForNextScan();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                bindUseCases(future.get());
            } catch (Exception e) {
                Snackbar.make(binding.getRoot(), R.string.something_went_wrong,
                        Snackbar.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        provider.unbindAll();
        provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null || scanningPaused) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        MLKitService.scanBarcode(image, barcodes -> {
            if (!barcodes.isEmpty() && !scanningPaused) {
                String value = barcodes.get(0).getRawValue();
                if (value != null) {
                    scanningPaused = true;
                    runOnUiThread(() -> viewModel.getProductByBarcode(value));
                }
            }
            imageProxy.close();
        }, e -> imageProxy.close());
    }

    private void showPanel(Product product) {
        binding.panel.setVisibility(View.VISIBLE);
        binding.tvProductName.setText(product.getName());
        binding.tvCurrentStock.setText(String.format(Locale.getDefault(),
                getString(R.string.current_stock), product.getStock()));
        binding.etQuantity.setText(String.valueOf(product.getStock()));
    }

    private void updateStock() {
        if (currentProduct == null) {
            return;
        }
        String qtyStr = binding.etQuantity.getText() == null ? "" :
                binding.etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            binding.tilQty.setError(getString(R.string.error_empty_field));
            return;
        }
        int newStock;
        try {
            newStock = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            binding.tilQty.setError(getString(R.string.error_empty_field));
            return;
        }
        viewModel.updateStock(currentProduct.getProductId(), newStock);
    }

    private void resetForNextScan() {
        currentProduct = null;
        binding.panel.setVisibility(View.GONE);
        binding.tilQty.setError(null);
        scanningPaused = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
