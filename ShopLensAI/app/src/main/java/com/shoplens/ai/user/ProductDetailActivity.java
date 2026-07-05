package com.shoplens.ai.user;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

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

        dialog.show();
    }
}
