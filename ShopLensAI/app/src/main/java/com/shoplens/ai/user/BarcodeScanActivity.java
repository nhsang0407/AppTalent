package com.shoplens.ai.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.ViewGroup;

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

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.shoplens.ai.R;
import com.shoplens.ai.ai.MLKitService;
import com.shoplens.ai.databinding.ActivityBarcodeScanBinding;
import com.shoplens.ai.utils.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight barcode scanner that returns the scanned value via RESULT_OK + EXTRA_BARCODE.
 */
public class BarcodeScanActivity extends AppCompatActivity {

    private ActivityBarcodeScanBinding binding;
    private ExecutorService cameraExecutor;
    private volatile boolean handled = false;

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
        binding = ActivityBarcodeScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int closeBtnOriginalMarginTop = ((ViewGroup.MarginLayoutParams) binding.btnClose.getLayoutParams()).topMargin;
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.btnClose.getLayoutParams();
            params.topMargin = closeBtnOriginalMarginTop + systemBars.top;
            binding.btnClose.setLayoutParams(params);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();
        binding.btnClose.setOnClickListener(v -> finish());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
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
        if (mediaImage == null || handled) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        MLKitService.scanBarcode(image, barcodes -> {
            if (!barcodes.isEmpty() && !handled) {
                String value = barcodes.get(0).getRawValue();
                if (value != null) {
                    handled = true;
                    runOnUiThread(() -> returnBarcode(value));
                }
            }
            imageProxy.close();
        }, e -> imageProxy.close());
    }

    private void returnBarcode(String barcode) {
        Intent data = new Intent();
        data.putExtra(Constants.EXTRA_BARCODE, barcode);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
