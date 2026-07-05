package com.shoplens.ai.user;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.ReviewAdapter;
import com.shoplens.ai.databinding.ActivityProductDetailBinding;
import com.shoplens.ai.databinding.BottomSheetReviewBinding;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.FirebaseUtils;
import com.shoplens.ai.viewmodel.CartViewModel;
import com.shoplens.ai.viewmodel.ProductViewModel;
import com.shoplens.ai.viewmodel.ReviewViewModel;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private ProductViewModel productViewModel;
    private ReviewViewModel reviewViewModel;
    private CartViewModel cartViewModel;
    private ReviewAdapter reviewAdapter;

    private String productId;
    private Product currentProduct;
    private int quantity = 1;
    private boolean summaryExpanded = false;
    private String currentUserName = "Anonymous";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int btnBackOriginalMarginTop = ((ViewGroup.MarginLayoutParams) binding.btnBack.getLayoutParams()).topMargin;
        final int bottomBarOriginalPaddingBottom = binding.bottomBar.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.rootProductDetail, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.btnBack.getLayoutParams();
            params.topMargin = btnBackOriginalMarginTop + systemBars.top;
            binding.btnBack.setLayoutParams(params);
            
            binding.bottomBar.setPadding(
                    binding.bottomBar.getPaddingLeft(),
                    binding.bottomBar.getPaddingTop(),
                    binding.bottomBar.getPaddingRight(),
                    bottomBarOriginalPaddingBottom + systemBars.bottom
            );
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(binding.rootProductDetail);

        productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        reviewAdapter = new ReviewAdapter();
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(reviewAdapter);

        loadCurrentUserName();
        observeViewModels();
        setupActions();

        if (productId != null) {
            productViewModel.getProductById(productId);
            reviewViewModel.loadReviews(productId);
        }
    }

    private void loadCurrentUserName() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            return;
        }
        FirebaseUtils.getDb().collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        currentUserName = name;
                    }
                });
    }

    private void observeViewModels() {
        productViewModel.getSelectedProduct().observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                bindProduct(product);
            }
        });

        productViewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        reviewViewModel.getReviews().observe(this, reviews -> {
            reviewAdapter.submit(reviews);
            binding.tvNoReviews.setVisibility(
                    (reviews == null || reviews.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        reviewViewModel.getAiSummary().observe(this, summary -> {
            binding.progressSummary.setVisibility(View.GONE);
            binding.tvSummary.setVisibility(View.VISIBLE);
            binding.tvSummaryHint.setVisibility(View.GONE);
            binding.tvSummary.setText(summary);
        });

        reviewViewModel.getSummaryLoading().observe(this, loading ->
                binding.progressSummary.setVisibility(loading ? View.VISIBLE : View.GONE));

        reviewViewModel.getReviewAdded().observe(this, added -> {
            if (Boolean.TRUE.equals(added)) {
                Snackbar.make(binding.getRoot(), R.string.review_added, Snackbar.LENGTH_SHORT).show();
            }
        });

        reviewViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindProduct(Product p) {
        binding.tvName.setText(p.getName());
        binding.tvPrice.setText(String.format(Locale.getDefault(),
                getString(R.string.price_format), p.getPrice()));
        binding.tvRating.setText(String.format(Locale.getDefault(),
                getString(R.string.rating_count_format), p.getAverageRating(), p.getReviewCount()));
        binding.tvDescription.setText(p.getDescription());

        if (p.isOutOfStock()) {
            binding.tvStock.setText(R.string.out_of_stock);
            binding.tvStock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.status_cancelled_bg)));
            binding.tvStock.setTextColor(getColor(R.color.stock_low));
            binding.btnAddToCart.setEnabled(false);
        } else {
            binding.tvStock.setText(String.format(Locale.getDefault(),
                    getString(R.string.in_stock), p.getStock()));
            binding.tvStock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.status_done_bg)));
            binding.tvStock.setTextColor(getColor(R.color.stock_ok));
            binding.btnAddToCart.setEnabled(true);
        }

        Glide.with(this)
                .load(p.getDisplayImageUrl())
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.ivProduct);
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
            }
        });
        binding.btnPlus.setOnClickListener(v -> {
            quantity++;
            binding.tvQuantity.setText(String.valueOf(quantity));
        });

        binding.btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) {
                return;
            }
            cartViewModel.addToCart(currentProduct, quantity);
            Snackbar.make(binding.getRoot(), R.string.added_to_cart, Snackbar.LENGTH_SHORT).show();
        });

        binding.cardAiSummary.setOnClickListener(v -> toggleSummary());

        binding.btnWriteReview.setOnClickListener(v -> showReviewSheet());
    }

    private void toggleSummary() {
        summaryExpanded = !summaryExpanded;
        binding.ivSummaryChevron.animate().rotation(summaryExpanded ? 180f : 0f).start();
        if (summaryExpanded && productId != null) {
            reviewViewModel.loadOrGenerateSummary(productId);
        }
    }

    private void showReviewSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_ShopLens_BottomSheet);
        BottomSheetReviewBinding sheet = BottomSheetReviewBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        // Setup AI actions
        sheet.btnReviewPoliteRewrite.setOnClickListener(v -> {
            String txt = sheet.etReview.getText() == null ? "" : sheet.etReview.getText().toString().trim();
            if (txt.isEmpty()) {
                sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                sheet.tvReviewAssistSuggestion.setText("Hãy nhập vài dòng review trước khi dùng tính năng này.");
                sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                }, 200);
            } else {
                reviewViewModel.assistReview(com.shoplens.ai.model.ReviewAssistAction.POLITE_REWRITE, txt, sheet.ratingInput.getRating(), currentProduct);
            }
        });

        sheet.btnReviewShorten.setOnClickListener(v -> {
            String txt = sheet.etReview.getText() == null ? "" : sheet.etReview.getText().toString().trim();
            if (txt.isEmpty()) {
                sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                sheet.tvReviewAssistSuggestion.setText("Hãy nhập vài dòng review trước khi dùng tính năng này.");
                sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                }, 200);
            } else {
                reviewViewModel.assistReview(com.shoplens.ai.model.ReviewAssistAction.SHORTEN, txt, sheet.ratingInput.getRating(), currentProduct);
            }
        });

        sheet.btnReviewProofread.setOnClickListener(v -> {
            String txt = sheet.etReview.getText() == null ? "" : sheet.etReview.getText().toString().trim();
            if (txt.isEmpty()) {
                sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                sheet.tvReviewAssistSuggestion.setText("Hãy nhập vài dòng review trước khi dùng tính năng này.");
                sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                }, 200);
            } else {
                reviewViewModel.assistReview(com.shoplens.ai.model.ReviewAssistAction.PROOFREAD, txt, sheet.ratingInput.getRating(), currentProduct);
            }
        });

        sheet.btnReviewSuggestFromRating.setOnClickListener(v -> {
            reviewViewModel.assistReview(com.shoplens.ai.model.ReviewAssistAction.SUGGEST_FROM_RATING, "", sheet.ratingInput.getRating(), currentProduct);
        });

        sheet.btnUseReviewSuggestion.setOnClickListener(v -> {
            String suggestion = sheet.tvReviewAssistSuggestion.getText().toString();
            if (!suggestion.isEmpty()) {
                sheet.etReview.setText(suggestion);
                sheet.layoutReviewAssistSuggestion.setVisibility(View.GONE);
            }
        });

        // Focus listener for smooth scrolling
        sheet.etReview.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.tilReview.getTop());
                }, 200);
            }
        });

        // Observe ViewModel LiveData
        reviewViewModel.getIsReviewAssistLoading().observe(this, loading -> {
            if (dialog.isShowing()) {
                sheet.progressReviewAssist.setVisibility(loading ? View.VISIBLE : View.GONE);
                if (loading) {
                    sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                    sheet.tvReviewAssistSuggestion.setText("Đang tạo gợi ý từ AI...");
                    sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                    sheet.btnReviewPoliteRewrite.setEnabled(false);
                    sheet.btnReviewShorten.setEnabled(false);
                    sheet.btnReviewProofread.setEnabled(false);
                    sheet.btnReviewSuggestFromRating.setEnabled(false);
                    
                    sheet.scrollReviewSheet.postDelayed(() -> {
                        sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                    }, 200);
                } else {
                    sheet.btnReviewPoliteRewrite.setEnabled(true);
                    sheet.btnReviewShorten.setEnabled(true);
                    sheet.btnReviewProofread.setEnabled(true);
                    sheet.btnReviewSuggestFromRating.setEnabled(true);
                }
            }
        });

        reviewViewModel.getReviewAssistSuggestion().observe(this, suggestion -> {
            if (dialog.isShowing() && suggestion != null) {
                sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                sheet.tvReviewAssistSuggestion.setText(suggestion);
                if (!suggestion.isEmpty() && !suggestion.startsWith("Hãy nhập vài dòng")) {
                    sheet.btnUseReviewSuggestion.setVisibility(View.VISIBLE);
                } else {
                    sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                }
                
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                }, 200);
            }
        });

        reviewViewModel.getReviewAssistError().observe(this, error -> {
            if (dialog.isShowing() && error != null) {
                sheet.layoutReviewAssistSuggestion.setVisibility(View.VISIBLE);
                sheet.tvReviewAssistSuggestion.setText("Hiện chưa thể tạo gợi ý bằng AI. Bạn có thể thử lại sau.");
                sheet.btnUseReviewSuggestion.setVisibility(View.GONE);
                
                sheet.scrollReviewSheet.postDelayed(() -> {
                    sheet.scrollReviewSheet.smoothScrollTo(0, sheet.layoutReviewAssistSuggestion.getTop());
                }, 200);
            }
        });

        dialog.setOnDismissListener(d -> {
            reviewViewModel.getReviewAssistSuggestion().setValue(null);
            reviewViewModel.getIsReviewAssistLoading().setValue(false);
            reviewViewModel.getReviewAssistError().setValue(null);
        });

        sheet.btnSubmitReview.setOnClickListener(v -> {
            String content = sheet.etReview.getText() == null ? "" :
                    sheet.etReview.getText().toString().trim();
            int rating = (int) sheet.ratingInput.getRating();
            if (content.isEmpty()) {
                sheet.tilReview.setError(getString(R.string.error_empty_field));
                return;
            }
            String uid = FirebaseUtils.getCurrentUserId();
            if (uid == null || productId == null) {
                return;
            }
            reviewViewModel.addReview(productId, uid, currentUserName, content, rating);
            dialog.dismiss();
        });

        // Set Soft Input resizing & configure BottomSheetBehavior
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                );
            }

            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<FrameLayout> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setFitToContents(false);
                behavior.setHalfExpandedRatio(0.9f);

                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(params);
            }
        });

        // Set WindowInsets listener to dynamically size spacer view matching keyboard height
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(sheet.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            boolean imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime());

            int spacerHeight = imeVisible ? ime.bottom : systemBars.bottom;
            ViewGroup.LayoutParams params = sheet.viewReviewImeSpacer.getLayoutParams();
            if (params.height != spacerHeight) {
                params.height = spacerHeight;
                sheet.viewReviewImeSpacer.setLayoutParams(params);
            }
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(sheet.getRoot());

        dialog.show();
    }
}
