package com.shoplens.ai.user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.shoplens.ai.R;
import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.ai.MLKitService;
import com.shoplens.ai.databinding.ActivityVisualSearchBinding;
import com.shoplens.ai.repository.SearchLogRepository;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;
import com.shoplens.ai.utils.ImageUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisualSearchActivity extends AppCompatActivity {

    private ActivityVisualSearchBinding binding;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private GeminiService geminiService;
    private final SearchLogRepository searchLogRepository = new SearchLogRepository();

    private volatile boolean barcodeMode = false;
    private volatile boolean handlingResult = false;
    private long lastLabelTime = 0;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.cd_camera, Snackbar.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVisualSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiService = new GeminiService(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnScanBarcode.setOnClickListener(v -> enableBarcodeMode());
        binding.btnIdentify.setOnClickListener(v -> identifyProduct());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void enableBarcodeMode() {
        barcodeMode = true;
        binding.scanFrame.setVisibility(View.VISIBLE);
        binding.tvHint.setText(R.string.scan_barcode_hint);
        binding.tvOverlayLabel.setVisibility(View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindUseCases(provider);
            } catch (Exception e) {
                Snackbar.make(binding.getRoot(), R.string.something_went_wrong,
                        Snackbar.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        provider.unbindAll();
        provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture, analysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null || handlingResult) {
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        if (barcodeMode) {
            MLKitService.scanBarcode(image, barcodes -> {
                if (!barcodes.isEmpty() && !handlingResult) {
                    String value = barcodes.get(0).getRawValue();
                    if (value != null) {
                        onBarcodeDetected(value);
                    }
                }
                imageProxy.close();
            }, e -> imageProxy.close());
        } else {
            long now = System.currentTimeMillis();
            if (now - lastLabelTime < 600) {
                imageProxy.close();
                return;
            }
            lastLabelTime = now;
            MLKitService.labelImage(image, labels -> {
                if (!labels.isEmpty()) {
                    String top = labels.get(0).getText();
                    runOnUiThread(() -> {
                        binding.tvOverlayLabel.setVisibility(View.VISIBLE);
                        binding.tvOverlayLabel.setText(top);
                    });
                }
                imageProxy.close();
            }, e -> imageProxy.close());
        }
    }

    private void onBarcodeDetected(String barcode) {
        if (handlingResult) {
            return;
        }
        handlingResult = true;
        logSearch(Constants.SEARCH_TYPE_BARCODE, barcode);
        runOnUiThread(() -> {
            Intent data = new Intent();
            data.putExtra(Constants.EXTRA_BARCODE, barcode);
            setResult(Constants.RESULT_BARCODE, data);
            finish();
        });
    }

    private void identifyProduct() {
        if (imageCapture == null || handlingResult) {
            return;
        }
        handlingResult = true;
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                Bitmap bitmap = imageProxyToBitmap(imageProxy);
                imageProxy.close();
                if (bitmap == null) {
                    fail();
                    return;
                }
                Bitmap scaled = ImageUtils.scaleDown(bitmap, 1024);
                runOnUiThread(() -> analyzeWithGemini(scaled));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                fail();
            }
        });
    }

    private void analyzeWithGemini(Bitmap bitmap) {
        geminiService.analyzeProductImage(bitmap, new GeminiService.GeminiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                String keyword = extractKeyword(result);
                logSearch(Constants.SEARCH_TYPE_VISUAL, keyword);
                binding.loadingOverlay.setVisibility(View.GONE);
                Intent data = new Intent();
                data.putExtra(Constants.EXTRA_SEARCH_QUERY, keyword);
                setResult(Constants.RESULT_VISUAL, data);
                finish();
            }

            @Override
            public void onError(Exception e) {
                binding.loadingOverlay.setVisibility(View.GONE);
                handlingResult = false;
                Snackbar.make(binding.getRoot(),
                        e.getMessage() != null ? e.getMessage()
                                : getString(R.string.something_went_wrong),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void fail() {
        runOnUiThread(() -> {
            binding.loadingOverlay.setVisibility(View.GONE);
            handlingResult = false;
            Snackbar.make(binding.getRoot(), R.string.something_went_wrong,
                    Snackbar.LENGTH_LONG).show();
        });
    }

    private void logSearch(String type, String label) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null || label == null || label.isEmpty()) {
            return;
        }
        searchLogRepository.logSearch(uid, type, label, new SearchLogRepository.VoidCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private static String extractKeyword(String json) {
        if (json == null) {
            return "";
        }
        String marker = "\"productName\"";
        int idx = json.indexOf(marker);
        if (idx >= 0) {
            int colon = json.indexOf(':', idx);
            int firstQuote = json.indexOf('"', colon + 1);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (firstQuote >= 0 && secondQuote > firstQuote) {
                return json.substring(firstQuote + 1, secondQuote);
            }
        }
        return json.split("\\r?\\n")[0].replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    private static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            return null;
        }
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
