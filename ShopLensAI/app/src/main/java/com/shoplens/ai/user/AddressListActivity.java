package com.shoplens.ai.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.AddressAdapter;
import com.shoplens.ai.databinding.ActivityAddressListBinding;
import com.shoplens.ai.model.Address;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.viewmodel.ProfileViewModel;

/**
 * Shows the user's saved addresses with options to add, edit, delete, and set default.
 */
public class AddressListActivity extends AppCompatActivity {

    private ActivityAddressListBinding binding;
    private ProfileViewModel profileViewModel;
    private AddressAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupEmptyState();
        observeViewModel();

        profileViewModel.loadAddresses();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnAdd.setOnClickListener(v -> openAddressForm(null));
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(new AddressAdapter.AddressActions() {
            @Override
            public void onEdit(Address address) {
                openAddressForm(address.getId());
            }

            @Override
            public void onDelete(Address address) {
                showDeleteDialog(address);
            }

            @Override
            public void onSetDefault(Address address) {
                profileViewModel.setDefaultAddress(address.getId());
            }
        });
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void setupEmptyState() {
        binding.btnAddFirst.setOnClickListener(v -> openAddressForm(null));
    }

    private void observeViewModel() {
        profileViewModel.getAddresses().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            binding.rvAddresses.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        profileViewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        profileViewModel.getSuccessMessage().observe(this, key -> {
            if (key == null) return;
            switch (key) {
                case "address_saved":
                    showSnackbar(getString(R.string.address_saved));
                    break;
                case "address_deleted":
                    showSnackbar(getString(R.string.address_deleted));
                    break;
                case "default_assigned":
                    showSnackbar(getString(R.string.default_assigned));
                    break;
            }
            profileViewModel.getSuccessMessage().setValue(null);
        });

        profileViewModel.getErrorMessage().observe(this, error -> {
            if (error == null) return;
            showSnackbar(error);
            profileViewModel.getErrorMessage().setValue(null);
        });
    }

    private void openAddressForm(@Nullable String addressId) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        if (addressId != null) {
            intent.putExtra(Constants.EXTRA_ADDRESS_ID, addressId);
        }
        startActivity(intent);
    }

    private void showDeleteDialog(Address address) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_address)
                .setMessage(R.string.delete_address_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) ->
                        profileViewModel.deleteAddress(address.getId()))
                .show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
