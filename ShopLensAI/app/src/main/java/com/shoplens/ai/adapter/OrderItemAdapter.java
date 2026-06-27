package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ItemOrderItemBinding;
import com.shoplens.ai.model.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder> {

    private final List<OrderItem> items = new ArrayList<>();

    public OrderItemAdapter() {
    }

    public OrderItemAdapter(List<OrderItem> initial) {
        submit(initial);
    }

    public void submit(List<OrderItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderItemBinding binding = ItemOrderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final ItemOrderItemBinding b;

        ItemViewHolder(ItemOrderItemBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(OrderItem item) {
            String priceFormat = itemView.getContext().getString(R.string.price_format);
            b.tvName.setText(item.getProductName());
            b.tvQtyPrice.setText(String.format(Locale.getDefault(), "%d × " + priceFormat,
                    item.getQuantity(), item.getPrice()));
            b.tvSubtotal.setText(String.format(Locale.getDefault(), priceFormat, item.getSubtotal()));
            Glide.with(itemView.getContext())
                    .load(item.getProductImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(b.ivProduct);
        }
    }
}
