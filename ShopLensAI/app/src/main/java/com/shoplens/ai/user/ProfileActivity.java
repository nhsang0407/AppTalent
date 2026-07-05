package com.shoplens.ai.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.auth.LoginActivity;
import com.shoplens.ai.databinding.ActivityProfileBinding;
import com.shoplens.ai.databinding.BottomSheetLanguageBinding;
import com.shoplens.ai.model.User;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.utils.ImageUtils;
import com.shoplens.ai.viewmodel.AuthViewModel;
import com.shoplens.ai.viewmodel.ProfileViewModel;

/**
 * Redesigned Profile screen — modern card-based Account Management Center.
 * Features: avatar upload, personal info edit, address management navigation,
 * language switching (EN/VI), and logout with confirmation.
 */
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private boolean isEditingInfo = false;
    private Uri cameraImageUri;

    // Gallery picker
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    profileViewModel.updateAvatar(uri);
                }
            });

    // Camera capture
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    profileViewModel.updateAvatar(cameraImageUri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupToolbar();
        setupAvatarPicker();
        setupPersonalInfoEdit();
        setupMenuItems();
        setupLogout();
        observeViewModel();

        profileViewModel.loadUser();
    }

    // --- Setup ---

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAvatarPicker() {
        View.OnClickListener avatarClick = v -> showAvatarPickerSheet();
        binding.frameAvatar.setOnClickListener(avatarClick);
        binding.ivAvatarEdit.setOnClickListener(avatarClick);
    }

    private void setupPersonalInfoEdit() {
        binding.btnEditInfo.setOnClickListener(v -> {
            if (isEditingInfo) {
                savePersonalInfo();
            } else {
                enterEditMode();
            }
        });
        binding.btnSaveInfo.setOnClickListener(v -> savePersonalInfo());
    }

    private void setupMenuItems() {
        binding.rowAddresses.setOnClickListener(v ->
                startActivity(new Intent(this, AddressListActivity.class)));

        binding.rowLanguage.setOnClickListener(v -> showLanguageSheet());
    }

    private void setupLogout() {
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // --- Observe ---

    private void observeViewModel() {
        profileViewModel.getUser().observe(this, this::populateUser);

        profileViewModel.getIsLoading().observe(this, loading ->
                binding.progressInfo.setVisibility(loading ? View.VISIBLE : View.GONE));

        profileViewModel.getAvatarUploading().observe(this, uploading -> {
            binding.progressAvatar.setVisibility(uploading ? View.VISIBLE : View.GONE);
            binding.ivAvatarEdit.setVisibility(uploading ? View.GONE : View.VISIBLE);
        });

        profileViewModel.getSuccessMessage().observe(this, key -> {
            if (key == null) return;
            switch (key) {
                case "profile_updated":
                    exitEditMode();
                    showSnackbar(getString(R.string.profile_updated));
                    break;
                case "avatar_updated":
                    showSnackbar(getString(R.string.avatar_updated));
                    break;
                case "avatar_removed":
                    showSnackbar(getString(R.string.avatar_removed));
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

    // --- User population ---

    private void populateUser(User user) {
        if (user == null) return;

        binding.tvName.setText(user.getName() != null ? user.getName() : "—");
        binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        binding.tvViewName.setText(user.getName() != null ? user.getName() : "—");
        binding.tvViewEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        binding.tvViewPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                ? user.getPhone() : "—");

        // Avatar — only load from Glide if avatarUrl is a valid https:// URL.
        // Any other value (null, empty, gs:// path) must show default avatar without
        // touching Firebase Storage (avoids "Object does not exist at location").
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && avatarUrl.startsWith("https://")) {
            android.util.Log.d("ProfileActivity", "populateUser: loading avatar from URL");
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.ivAvatar);
            binding.ivAvatar.setPadding(0, 0, 0, 0);
            binding.ivAvatar.clearColorFilter();
        } else {
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                android.util.Log.w("ProfileActivity",
                        "populateUser: invalid avatarUrl (not https://), showing default. url=" + avatarUrl);
            }
            // Show default person icon with padding
            binding.ivAvatar.setPadding(
                    (int) getResources().getDimension(R.dimen.space_l),
                    (int) getResources().getDimension(R.dimen.space_l),
                    (int) getResources().getDimension(R.dimen.space_l),
                    (int) getResources().getDimension(R.dimen.space_l));
            binding.ivAvatar.setImageResource(R.drawable.ic_person);
        }

        // Language label
        String lang = profileViewModel.getSavedLanguage();
        binding.tvCurrentLanguage.setText(
                Constants.LANG_VI.equals(lang)
                        ? getString(R.string.language_vietnamese)
                        : getString(R.string.language_english));
    }

    // --- Edit mode ---

    private void enterEditMode() {
        isEditingInfo = true;
        User user = profileViewModel.getUser().getValue();
        if (user != null) {
            binding.etName.setText(user.getName());
            binding.etPhone.setText(user.getPhone());
        }
        binding.layoutViewName.setVisibility(View.GONE);
        binding.layoutViewPhone.setVisibility(View.GONE);
        binding.tilName.setVisibility(View.VISIBLE);
        binding.tilPhone.setVisibility(View.VISIBLE);
        binding.btnSaveInfo.setVisibility(View.VISIBLE);
        binding.btnEditInfo.setText(getString(R.string.cancel));
    }

    private void exitEditMode() {
        isEditingInfo = false;
        binding.layoutViewName.setVisibility(View.VISIBLE);
        binding.layoutViewPhone.setVisibility(View.VISIBLE);
        binding.tilName.setVisibility(View.GONE);
        binding.tilPhone.setVisibility(View.GONE);
        binding.btnSaveInfo.setVisibility(View.GONE);
        binding.tilName.setError(null);
        binding.tilPhone.setError(null);
        binding.btnEditInfo.setText(getString(R.string.edit));
    }

    private void savePersonalInfo() {
        if (isEditingInfo) {
            // If in cancel mode
            String name = binding.etName.getText() == null ? "" : binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText() == null ? "" : binding.etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.tilName.setError(getString(R.string.error_name_required));
                return;
            }
            if (!TextUtils.isEmpty(phone) && !isValidPhone(phone)) {
                binding.tilPhone.setError(getString(R.string.error_invalid_phone));
                return;
            }
            binding.tilName.setError(null);
            binding.tilPhone.setError(null);
            profileViewModel.updatePersonalInfo(name, phone);
        } else {
            exitEditMode();
        }
    }

    private boolean isValidPhone(String phone) {
        return phone.replaceAll("[\\s\\-+()]", "").matches("\\d{7,15}");
    }

    // --- Avatar picker ---

    private void showAvatarPickerSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_ShopLens_BottomSheet);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_avatar_picker, null);
        dialog.setContentView(view);

        view.findViewById(R.id.rowGallery).setOnClickListener(v -> {
            dialog.dismiss();
            galleryLauncher.launch("image/*");
        });
        view.findViewById(R.id.rowCamera).setOnClickListener(v -> {
            dialog.dismiss();
            cameraImageUri = ImageUtils.createCameraImageUri(this);
            if (cameraImageUri != null) {
                cameraLauncher.launch(cameraImageUri);
            }
        });
        view.findViewById(R.id.rowRemove).setOnClickListener(v -> {
            dialog.dismiss();
            profileViewModel.removeAvatar();
        });

        dialog.show();
    }

    // --- Language ---

    private void showLanguageSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.ThemeOverlay_ShopLens_BottomSheet);
        BottomSheetLanguageBinding sheet = BottomSheetLanguageBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        String currentLang = profileViewModel.getSavedLanguage();
        sheet.icCheckEnglish.setVisibility(Constants.LANG_EN.equals(currentLang) ? View.VISIBLE : View.GONE);
        sheet.icCheckVietnamese.setVisibility(Constants.LANG_VI.equals(currentLang) ? View.VISIBLE : View.GONE);

        sheet.rowEnglish.setOnClickListener(v -> {
            dialog.dismiss();
            profileViewModel.setLanguage(Constants.LANG_EN);
            // Recreate after locale change
            recreate();
        });
        sheet.rowVietnamese.setOnClickListener(v -> {
            dialog.dismiss();
            profileViewModel.setLanguage(Constants.LANG_VI);
            recreate();
        });

        dialog.show();
    }

    // --- Logout ---

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_logout)
                .setMessage(R.string.confirm_logout_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> performLogout())
                .show();
    }

    private void performLogout() {
        authViewModel.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- Helpers ---

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
