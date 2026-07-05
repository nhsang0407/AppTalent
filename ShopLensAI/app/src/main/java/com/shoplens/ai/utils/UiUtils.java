package com.shoplens.ai.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utility to manage Safe Area (WindowInsets) insets for views to prevent overlap
 * with status bar, notch, and navigation bar.
 */
public class UiUtils {

    public static void setupSystemBarInsets(
            @NonNull Window window,
            @NonNull View root,
            @NonNull View toolbar,
            @NonNull View bottomView,
            @NonNull View fab
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false);

        final int topOriginalPaddingLeft = toolbar.getPaddingLeft();
        final int topOriginalPaddingTop = toolbar.getPaddingTop();
        final int topOriginalPaddingRight = toolbar.getPaddingRight();
        final int topOriginalPaddingBottom = toolbar.getPaddingBottom();

        final int bottomOriginalPaddingLeft = bottomView.getPaddingLeft();
        final int bottomOriginalPaddingTop = bottomView.getPaddingTop();
        final int bottomOriginalPaddingRight = bottomView.getPaddingRight();
        final int bottomOriginalPaddingBottom = bottomView.getPaddingBottom();

        final ViewGroup.MarginLayoutParams fabOriginalMargins =
                fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                        ? (ViewGroup.MarginLayoutParams) fab.getLayoutParams()
                        : null;

        final int fabOriginalBottomMargin = fabOriginalMargins != null ? fabOriginalMargins.bottomMargin : 0;
        final int fabOriginalRightMargin = fabOriginalMargins != null ? fabOriginalMargins.rightMargin : 0;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            toolbar.setPadding(
                    topOriginalPaddingLeft,
                    topOriginalPaddingTop + systemBars.top,
                    topOriginalPaddingRight,
                    topOriginalPaddingBottom
            );

            bottomView.setPadding(
                    bottomOriginalPaddingLeft,
                    bottomOriginalPaddingTop,
                    bottomOriginalPaddingRight,
                    bottomOriginalPaddingBottom + systemBars.bottom
            );

            if (fabOriginalMargins != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                params.bottomMargin = fabOriginalBottomMargin + systemBars.bottom;
                params.rightMargin = fabOriginalRightMargin + systemBars.right;
                fab.setLayoutParams(params);
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    public static void applyTopInsetToToolbar(@NonNull Window window, @NonNull View root, @NonNull View toolbar) {
        WindowCompat.setDecorFitsSystemWindows(window, false);

        final int left = toolbar.getPaddingLeft();
        final int top = toolbar.getPaddingTop();
        final int right = toolbar.getPaddingRight();
        final int bottom = toolbar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(left, top + systemBars.top, right, bottom);
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    public static void applyTopAndBottomInsets(
            @NonNull Window window,
            @NonNull View root,
            @NonNull View toolbar,
            @NonNull View bottomView
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false);

        final int leftTop = toolbar.getPaddingLeft();
        final int topTop = toolbar.getPaddingTop();
        final int rightTop = toolbar.getPaddingRight();
        final int bottomTop = toolbar.getPaddingBottom();

        final int leftBottom = bottomView.getPaddingLeft();
        final int topBottom = bottomView.getPaddingTop();
        final int rightBottom = bottomView.getPaddingRight();
        final int bottomBottom = bottomView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(leftTop, topTop + systemBars.top, rightTop, bottomTop);
            bottomView.setPadding(leftBottom, topBottom, rightBottom, bottomBottom + systemBars.bottom);
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}
