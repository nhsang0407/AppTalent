package com.shoplens.ai.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bitmap <-> Uri helpers used for camera capture and AI image generation.
 */
public final class ImageUtils {

    private ImageUtils() {
    }

    /** Loads a bitmap from a content Uri. */
    @Nullable
    public static Bitmap uriToBitmap(@NonNull Context context, @NonNull Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            return null;
        }
    }

    /** Compresses a bitmap to JPEG bytes at the given quality (0..100). */
    @NonNull
    public static byte[] bitmapToJpegBytes(@NonNull Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }

    /**
     * Writes a bitmap to a temp file in the cache dir and returns a FileProvider Uri for it.
     * Useful for uploading an AI-generated bitmap via {@code uploadImageToStorage}.
     */
    @Nullable
    public static Uri bitmapToCacheUri(@NonNull Context context, @NonNull Bitmap bitmap) {
        try {
            File cacheDir = new File(context.getCacheDir(), "images");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                return null;
            }
            File file = new File(cacheDir, "ai_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            return androidx.core.content.FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            return null;
        }
    }

    /** Creates an empty temp file in cache/images and returns its FileProvider Uri (for camera intent). */
    @Nullable
    public static Uri createCameraImageUri(@NonNull Context context) {
        try {
            File cacheDir = new File(context.getCacheDir(), "images");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                return null;
            }
            File file = new File(cacheDir, "capture_" + System.currentTimeMillis() + ".jpg");
            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            }
            return androidx.core.content.FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            return null;
        }
    }

    /** Scales a bitmap down so its longest side is at most maxSize, preserving aspect ratio. */
    @NonNull
    public static Bitmap scaleDown(@NonNull Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}
