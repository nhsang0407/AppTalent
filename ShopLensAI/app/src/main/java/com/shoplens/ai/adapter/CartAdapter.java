package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ItemCartBinding;
import com.shoplens.ai.model.CartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartListener {
        void onQuantityChange(String productId, int newQuantity);

        void onRemove(String productId);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final CartListener listener;

    public CartAdapter(CartListener listener) {
        this.listener = listener;
    }

    public void submit(List<CartItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        final ItemCartBinding b;

        CartViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(CartItem item) {
            String productId = item.getProduct().getProductId();
            b.tvName.setText(item.getProduct().getName());
            b.tvQuantity.setText(String.valueOf(item.getQuantity()));
            b.tvSubtotal.setText(String.format(Locale.getDefault(),
                    itemView.getContext().getString(R.string.price_format), item.getSubtotal()));

            Glide.with(itemView.getContext())
                    .load(item.getProduct().getDisplayImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(b.ivProduct);

            b.btnMinus.setOnClickListener(v ->
                    listener.onQuantityChange(productId, item.getQuantity() - 1));
            b.btnPlus.setOnClickListener(v ->
                    listener.onQuantityChange(productId, item.getQuantity() + 1));
            b.btnRemove.setOnClickListener(v -> listener.onRemove(productId));
        }
    }
}
