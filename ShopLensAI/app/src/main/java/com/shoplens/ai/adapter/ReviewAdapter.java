package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shoplens.ai.databinding.ItemReviewBinding;
import com.shoplens.ai.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private final List<Review> reviews = new ArrayList<>();

    public void submit(List<Review> newReviews) {
        reviews.clear();
        if (newReviews != null) {
            reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final ItemReviewBinding b;

        ReviewViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Review r) {
            b.tvUser.setText(r.getUserName());
            b.tvContent.setText(r.getContent());
            b.ratingBar.setRating(r.getRating());
            if (r.getCreatedAt() != null) {
                b.tvDate.setText(DATE_FORMAT.format(r.getCreatedAt().toDate()));
            } else {
                b.tvDate.setText("");
            }
        }
    }
}
