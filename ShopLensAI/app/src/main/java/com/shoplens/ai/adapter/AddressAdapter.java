package com.shoplens.ai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.shoplens.ai.databinding.ItemAddressBinding;
import com.shoplens.ai.model.Address;

/**
 * RecyclerView adapter for the address list screen.
 */
public class AddressAdapter extends ListAdapter<Address, AddressAdapter.ViewHolder> {

    public interface AddressActions {
        void onEdit(Address address);
        void onDelete(Address address);
        void onSetDefault(Address address);
    }

    private final AddressActions actions;

    public AddressAdapter(@NonNull AddressActions actions) {
        super(DIFF_CALLBACK);
        this.actions = actions;
    }

    private static final DiffUtil.ItemCallback<Address> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Address>() {
                @Override
                public boolean areItemsTheSame(@NonNull Address oldItem, @NonNull Address newItem) {
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Address oldItem, @NonNull Address newItem) {
                    return oldItem.getId() != null
                            && oldItem.getId().equals(newItem.getId())
                            && oldItem.isDefault() == newItem.isDefault()
                            && String.valueOf(oldItem.getDetail()).equals(String.valueOf(newItem.getDetail()))
                            && String.valueOf(oldItem.getReceiverName()).equals(String.valueOf(newItem.getReceiverName()));
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressBinding binding = ItemAddressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAddressBinding b;

        ViewHolder(ItemAddressBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Address address) {
            b.tvLabel.setText(address.getLabel() != null ? address.getLabel() : "Other");
            b.tvDefault.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
            b.tvReceiverName.setText(address.getReceiverName());
            b.tvPhone.setText(address.getPhone());
            b.tvFullAddress.setText(address.getFullAddress());

            // Hide "Set Default" if already default
            b.btnSetDefault.setVisibility(address.isDefault() ? View.GONE : View.VISIBLE);

            b.btnEdit.setOnClickListener(v -> actions.onEdit(address));
            b.btnDelete.setOnClickListener(v -> actions.onDelete(address));
            b.btnSetDefault.setOnClickListener(v -> actions.onSetDefault(address));
        }
    }
}
