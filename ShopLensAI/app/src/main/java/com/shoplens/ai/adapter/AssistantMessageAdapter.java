package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shoplens.ai.databinding.ItemAssistantMessageBinding;
import com.shoplens.ai.databinding.ItemRecommendationCardBinding;
import com.shoplens.ai.model.AssistantMessage;
import com.shoplens.ai.model.AssistantRecommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to render chat bubbles and dynamic recommendation cards.
 */
public class AssistantMessageAdapter extends RecyclerView.Adapter<AssistantMessageAdapter.MessageViewHolder> {

    public interface OnRecommendationClickListener {
        void onRecommendationClick(String productId);
    }

    private final List<AssistantMessage> messages = new ArrayList<>();
    private final OnRecommendationClickListener listener;

    public AssistantMessageAdapter(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AssistantMessage> newList) {
        messages.clear();
        if (newList != null) {
            messages.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAssistantMessageBinding binding = ItemAssistantMessageBinding.inflate(inflater, parent, false);
        return new MessageViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemAssistantMessageBinding binding;
        private final OnRecommendationClickListener listener;

        MessageViewHolder(ItemAssistantMessageBinding binding, OnRecommendationClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(AssistantMessage msg) {
            if (msg.isFromUser()) {
                binding.llUserMessage.setVisibility(View.VISIBLE);
                binding.llAssistantMessage.setVisibility(View.GONE);
                binding.tvUserContent.setText(msg.getContent());
            } else {
                binding.llUserMessage.setVisibility(View.GONE);
                binding.llAssistantMessage.setVisibility(View.VISIBLE);
                binding.tvAssistantContent.setText(msg.getContent());

                binding.llRecommendations.removeAllViews();

                List<AssistantRecommendation> recs = msg.getRecommendations();
                if (recs != null && !recs.isEmpty()) {
                    binding.llRecommendations.setVisibility(View.VISIBLE);
                    LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
                    for (AssistantRecommendation rec : recs) {
                        ItemRecommendationCardBinding cardBinding =
                                ItemRecommendationCardBinding.inflate(inflater, binding.llRecommendations, false);
                        
                        cardBinding.tvRecName.setText(rec.getProductName());
                        cardBinding.tvRecPrice.setText(rec.getPriceText());
                        cardBinding.tvRecStock.setText(rec.getStockText());
                        cardBinding.tvRecReason.setText(rec.getReason());

                        cardBinding.getRoot().setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onRecommendationClick(rec.getProductId());
                            }
                        });

                        binding.llRecommendations.addView(cardBinding.getRoot());
                    }
                } else {
                    binding.llRecommendations.setVisibility(View.GONE);
                }
            }
        }
    }
}
