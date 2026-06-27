package com.shoplens.ai.ai;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.List;

/**
 * Wraps the on-device ML Kit detectors used by the visual-search and barcode flows.
 */
public final class MLKitService {

    private MLKitService() {
    }

    private static final BarcodeScanner BARCODE_SCANNER = BarcodeScanning.getClient();

    private static final ObjectDetector OBJECT_DETECTOR = ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .build());

    private static final ImageLabeler IMAGE_LABELER = ImageLabeling.getClient(
            new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.6f)
                    .build());

    private static final SmartReplyGenerator SMART_REPLY = SmartReply.getClient();

    /** Detects barcodes in the supplied frame. */
    public static void scanBarcode(@NonNull InputImage image,
                                   @NonNull OnSuccessListener<List<Barcode>> onSuccess,
                                   @NonNull OnFailureListener onFailure) {
        BARCODE_SCANNER.process(image)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Runs streaming object detection on the supplied frame. */
    public static void detectObjects(@NonNull InputImage image,
                                     @NonNull OnSuccessListener<List<DetectedObject>> onSuccess,
                                     @NonNull OnFailureListener onFailure) {
        OBJECT_DETECTOR.process(image)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Returns image labels for the supplied frame. */
    public static void labelImage(@NonNull InputImage image,
                                  @NonNull OnSuccessListener<List<ImageLabel>> onSuccess,
                                  @NonNull OnFailureListener onFailure) {
        IMAGE_LABELER.process(image)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Generates smart-reply suggestions for a conversation. */
    public static void getSmartReplies(@NonNull List<TextMessage> conversation,
                                       @NonNull OnSuccessListener<SmartReplySuggestionResult> onSuccess,
                                       @NonNull OnFailureListener onFailure) {
        SMART_REPLY.suggestReplies(conversation)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
