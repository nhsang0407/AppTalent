package com.shoplens.ai.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ItemOrderBinding;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onStatusChange(Order order, String newStatus);

        void onOrderClick(Order order);
    }

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final List<String> STATUSES = Arrays.asList(
            Constants.STATUS_PENDING, Constants.STATUS_CONFIRMED,
            Constants.STATUS_DONE, Constants.STATUS_CANCELLED);

    private final List<Order> orders = new ArrayList<>();
    private final boolean adminMode;
    private final OnOrderActionListener listener;

    public OrderAdapter(boolean adminMode, OnOrderActionListener listener) {
        this.adminMode = adminMode;
        this.listener = listener;
    }

    public void submit(List<Order> newOrders) {
        orders.clear();
        if (newOrders != null) {
            orders.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        final ItemOrderBinding b;
        boolean expanded = false;

        OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.b = binding;
            b.rvItems.setLayoutManager(
                    new LinearLayoutManager(binding.getRoot().getContext()));
        }

        void bind(Order order) {
            String shortId = order.getOrderId() != null && order.getOrderId().length() > 6
                    ? order.getOrderId().substring(0, 6).toUpperCase(Locale.getDefault())
                    : String.valueOf(order.getOrderId());
            b.tvOrderId.setText(itemView.getContext().getString(R.string.order_id, shortId));

            if (order.getCreatedAt() != null) {
                b.tvDate.setText(DATE_FORMAT.format(order.getCreatedAt().toDate()));
            } else {
                b.tvDate.setText("");
            }
            b.tvTotal.setText(String.format(Locale.getDefault(),
                    itemView.getContext().getString(R.string.price_format), order.getTotalPrice()));

            applyStatusChip(order.getStatus());

            // Items
            OrderItemAdapter itemAdapter = new OrderItemAdapter(order.getItems());
            b.rvItems.setAdapter(itemAdapter);

            expanded = false;
            b.rvItems.setVisibility(View.GONE);
            b.ivExpand.setRotation(0f);

            View.OnClickListener toggle = v -> {
                expanded = !expanded;
                b.rvItems.setVisibility(expanded ? View.VISIBLE : View.GONE);
                b.ivExpand.animate().rotation(expanded ? 180f : 0f).start();
                listener.onOrderClick(order);
            };
            b.getRoot().setOnClickListener(toggle);

            if (adminMode) {
                b.tvCustomer.setVisibility(View.VISIBLE);
                b.tvCustomer.setText(itemView.getContext()
                        .getString(R.string.customer, order.getUserName()));
                b.llAdminStatus.setVisibility(View.VISIBLE);
                setupStatusSpinner(order);
            } else {
                b.tvCustomer.setVisibility(View.GONE);
                b.llAdminStatus.setVisibility(View.GONE);
            }
        }

        private void setupStatusSpinner(Order order) {
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    itemView.getContext(), android.R.layout.simple_spinner_item, STATUSES);
            spinnerAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            b.spinnerStatus.setAdapter(spinnerAdapter);

            int index = STATUSES.indexOf(order.getStatus());
            if (index < 0) {
                index = 0;
            }
            b.spinnerStatus.setSelection(index, false);

            final int initialIndex = index;
            b.spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean first = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (first) {
                        first = false;
                        return;
                    }
                    if (pos != initialIndex) {
                        listener.onStatusChange(order, STATUSES.get(pos));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void applyStatusChip(String status) {
            int bg;
            int fg;
            int label;
            if (Constants.STATUS_CONFIRMED.equals(status)) {
                bg = R.color.status_confirmed_bg;
                fg = R.color.status_confirmed;
                label = R.string.status_confirmed;
            } else if (Constants.STATUS_DONE.equals(status)) {
                bg = R.color.status_done_bg;
                fg = R.color.status_done;
                label = R.string.status_done;
            } else if (Constants.STATUS_CANCELLED.equals(status)) {
                bg = R.color.status_cancelled_bg;
                fg = R.color.status_cancelled;
                label = R.string.status_cancelled;
            } else {
                bg = R.color.status_pending_bg;
                fg = R.color.status_pending;
                label = R.string.status_pending;
            }
            b.tvStatus.setText(label);
            b.tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(itemView.getContext().getColor(bg)));
            b.tvStatus.setTextColor(itemView.getContext().getColor(fg));
        }
    }
}
