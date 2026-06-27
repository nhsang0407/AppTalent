package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shoplens.ai.databinding.ItemSearchTrendBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchTrendAdapter extends RecyclerView.Adapter<SearchTrendAdapter.TrendViewHolder> {

    private final List<Map.Entry<String, Integer>> items = new ArrayList<>();
    private int maxCount = 1;

    public void submit(List<Map.Entry<String, Integer>> newItems) {
        items.clear();
        maxCount = 1;
        if (newItems != null) {
            items.addAll(newItems);
            for (Map.Entry<String, Integer> e : newItems) {
                maxCount = Math.max(maxCount, e.getValue());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchTrendBinding binding = ItemSearchTrendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendViewHolder holder, int position) {
        holder.bind(items.get(position), maxCount);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TrendViewHolder extends RecyclerView.ViewHolder {
        final ItemSearchTrendBinding b;

        TrendViewHolder(ItemSearchTrendBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Map.Entry<String, Integer> entry, int maxCount) {
            b.tvLabel.setText(entry.getKey());
            b.tvCount.setText(String.valueOf(entry.getValue()));

            float fraction = maxCount > 0 ? (float) entry.getValue() / maxCount : 0f;
            fraction = Math.max(0.02f, Math.min(1f, fraction));

            LinearLayout.LayoutParams fillParams =
                    (LinearLayout.LayoutParams) b.barFill.getLayoutParams();
            fillParams.weight = fraction;
            b.barFill.setLayoutParams(fillParams);

            LinearLayout.LayoutParams spaceParams =
                    (LinearLayout.LayoutParams) b.barSpace.getLayoutParams();
            spaceParams.weight = 1f - fraction;
            b.barSpace.setLayoutParams(spaceParams);
        }
    }
}
