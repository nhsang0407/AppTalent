package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ItemProductGridBinding;
import com.shoplens.ai.databinding.ItemProductListBinding;
import com.shoplens.ai.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders products either as a grid card (storefront) or a list row (admin management).
 */
public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);

        default void onEditClick(Product product) {
        }

        default void onDeleteClick(Product product) {
        }
    }

    private static final int MODE_GRID = 0;
    private static final int MODE_LIST = 1;

    private final List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;
    private final boolean listMode;
    private final boolean showActions;

    public ProductAdapter(boolean listMode, OnProductClickListener listener) {
        this(listMode, true, listener);
    }

    public ProductAdapter(boolean listMode, boolean showActions, OnProductClickListener listener) {
        this.listMode = listMode;
        this.showActions = showActions;
        this.listener = listener;
    }

    public void submit(List<Product> newProducts) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return listMode ? MODE_LIST : MODE_GRID;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MODE_LIST) {
            return new ListViewHolder(ItemProductListBinding.inflate(inflater, parent, false));
        }
        return new GridViewHolder(ItemProductGridBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Product product = products.get(position);
        if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(product);
        } else {
            ((ListViewHolder) holder).bind(product);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private static String price(String format, double value) {
        return String.format(Locale.getDefault(), format, value);
    }

    // ---------------- Grid ----------------
    class GridViewHolder extends RecyclerView.ViewHolder {
        final ItemProductGridBinding b;

        GridViewHolder(ItemProductGridBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Product p) {
            b.tvName.setText(p.getName());
            b.tvPrice.setText(price(itemView.getContext().getString(R.string.price_format), p.getPrice()));
            b.tvRating.setText(String.format(Locale.getDefault(), "%.1f", p.getAverageRating()));
            Glide.with(itemView.getContext())
                    .load(p.getDisplayImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(b.ivProduct);
            b.overlayOutOfStock.setVisibility(p.isOutOfStock() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> listener.onProductClick(p));
        }
    }

    // ---------------- List ----------------
    class ListViewHolder extends RecyclerView.ViewHolder {
        final ItemProductListBinding b;

        ListViewHolder(ItemProductListBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Product p) {
            b.tvName.setText(p.getName());
            b.tvPrice.setText(price(itemView.getContext().getString(R.string.price_format), p.getPrice()));
            b.tvStock.setText(itemView.getContext().getString(R.string.stock) + ": " + p.getStock());

            boolean low = p.isLowStock();
            int bgColor = itemView.getContext().getColor(low ? R.color.status_cancelled_bg : R.color.status_done_bg);
            int textColor = itemView.getContext().getColor(low ? R.color.stock_low : R.color.stock_ok);
            b.tvStock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
            b.tvStock.setTextColor(textColor);

            Glide.with(itemView.getContext())
                    .load(p.getDisplayImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(b.ivProduct);

            int actionVisibility = showActions ? android.view.View.VISIBLE : android.view.View.GONE;
            b.btnEdit.setVisibility(actionVisibility);
            b.btnDelete.setVisibility(actionVisibility);

            itemView.setOnClickListener(v -> listener.onProductClick(p));
            b.btnEdit.setOnClickListener(v -> listener.onEditClick(p));
            b.btnDelete.setOnClickListener(v -> listener.onDeleteClick(p));
        }
    }
}
