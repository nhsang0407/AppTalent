package com.shoplens.ai.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.databinding.ActivityAddressFormBinding;
import com.shoplens.ai.model.Address;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.viewmodel.ProfileViewModel;

import java.util.List;

/**
 * Add or Edit an address form. Receives an optional addressId via Intent extras
 * to pre-fill fields for editing.
 */
public class AddressFormActivity extends AppCompatActivity {

    private ActivityAddressFormBinding binding;
    private ProfileViewModel profileViewModel;

    @Nullable
    private String editAddressId;
    @Nullable
    private Address existingAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        editAddressId = getIntent().getStringExtra(Constants.EXTRA_ADDRESS_ID);

        setupToolbar();
        observeViewModel();

        if (editAddressId != null) {
            binding.toolbar.setTitle(getString(R.string.edit_address));
            profileViewModel.loadAddresses(); // Load to find the address
        }

        binding.btnSave.setOnClickListener(v -> saveAddress());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        profileViewModel.getAddresses().observe(this, list -> {
            if (editAddressId != null && existingAddress == null) {
                // Find and pre-fill the address to edit
                for (Address a : list) {
                    if (editAddressId.equals(a.getId())) {
                        existingAddress = a;
                        prefillForm(a);
                        break;
                    }
                }
            }
        });

        profileViewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        profileViewModel.getSuccessMessage().observe(this, key -> {
            if ("address_saved".equals(key)) {
                profileViewModel.getSuccessMessage().setValue(null);
                finish();
            }
        });

        profileViewModel.getErrorMessage().observe(this, error -> {
            if (error == null) return;
            Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            profileViewModel.getErrorMessage().setValue(null);
        });
    }

    private void prefillForm(Address address) {
        binding.etReceiverName.setText(address.getReceiverName());
        binding.etPhone.setText(address.getPhone());
        binding.etDetail.setText(address.getDetail());
        binding.etWard.setText(address.getWard());
        binding.etDistrict.setText(address.getDistrict());
        binding.etCity.setText(address.getCity());
        binding.switchDefault.setChecked(address.isDefault());

        // Label chip
        String label = address.getLabel();
        if ("Work".equals(label)) {
            binding.chipWork.setChecked(true);
        } else if ("Other".equals(label)) {
            binding.chipOther.setChecked(true);
        } else {
            binding.chipHome.setChecked(true);
        }
    }

    private void saveAddress() {
        String receiverName = getText(binding.etReceiverName);
        String phone = getText(binding.etPhone);
        String detail = getText(binding.etDetail);
        String ward = getText(binding.etWard);
        String district = getText(binding.etDistrict);
        String city = getText(binding.etCity);

        // Validation
        boolean valid = true;
        if (TextUtils.isEmpty(receiverName)) {
            binding.tilReceiverName.setError(getString(R.string.error_empty_field));
            valid = false;
        } else {
            binding.tilReceiverName.setError(null);
        }
        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.setError(getString(R.string.error_empty_field));
            valid = false;
        } else if (!phone.replaceAll("[\\s\\-+()]", "").matches("\\d{7,15}")) {
            binding.tilPhone.setError(getString(R.string.error_invalid_phone));
            valid = false;
        } else {
            binding.tilPhone.setError(null);
        }
        if (TextUtils.isEmpty(detail)) {
            binding.tilDetail.setError(getString(R.string.address_required));
            valid = false;
        } else {
            binding.tilDetail.setError(null);
        }
        if (TextUtils.isEmpty(city)) {
            binding.tilCity.setError(getString(R.string.city_required));
            valid = false;
        } else {
            binding.tilCity.setError(null);
        }

        if (!valid) return;

        // Determine label
        String label;
        if (binding.chipWork.isChecked()) {
            label = "Work";
        } else if (binding.chipOther.isChecked()) {
            label = "Other";
        } else {
            label = "Home";
        }

        boolean isDefault = binding.switchDefault.isChecked();

        Address address = new Address(receiverName, phone, detail, ward, district, city, label, isDefault);

        if (editAddressId != null) {
            address.setId(editAddressId);
            profileViewModel.updateAddress(address);
        } else {
            profileViewModel.addAddress(address);
        }
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
