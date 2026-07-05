# App System Bars UI Fix Test Report

## Summary
- **Feature fixed**: Status bar, notch, and navigation bar Safe Area overlap fixes (WindowInsets integration)
- **Language used**: Java
- **Files changed**:
  - [UiUtils.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/utils/UiUtils.java) [NEW]
  - [activity_home.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_home.xml) [MODIFY]
  - [HomeActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/HomeActivity.java) [MODIFY]
  - [activity_admin_dashboard.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_admin_dashboard.xml) [MODIFY]
  - [AdminDashboardActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/admin/AdminDashboardActivity.java) [MODIFY]
  - [activity_product_detail.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_product_detail.xml) [MODIFY]
  - [ProductDetailActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ProductDetailActivity.java) [MODIFY]
  - [BarcodeStockActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/admin/BarcodeStockActivity.java) [MODIFY]
  - [BarcodeScanActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/BarcodeScanActivity.java) [MODIFY]
  - [VisualSearchActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/VisualSearchActivity.java) [MODIFY]
  - Layouts updated: `activity_cart.xml`, `activity_checkout.xml`, `activity_profile.xml`, `activity_order_history.xml`, `activity_product_management.xml`, `activity_add_edit_product.xml`, `activity_order_management.xml`, `activity_search_trend.xml`, `activity_login.xml`, `activity_register.xml`.
- **Build status**: `BUILD SUCCESSFUL` for both unit test suite and debug APK packaging.

## Root Cause
- **Why top app bar was overlapping status bar**: Due to Android 15 (targetSdk 35) forcing edge-to-edge rendering by default for all activities.
- **Insets configuration**: Without explicit safe area fitsSystemWindows parameters or insets listener padding, toolbars and bottom buttons were drawn directly underneath system status bars, notches, and navigation bars.

## Implementation Details
- **UiUtils safe area manager**: Exposes centralized insets callbacks (`setupSystemBarInsets`, `applyTopInsetToToolbar`, `applyTopAndBottomInsets`) to query system bounds dynamically.
- **HomeActivity inset handling**: Binds bottom navigation padding and camera FAB margins above system navigation bar. Changes custom toolbar to `wrap_content` height and `?attr/actionBarSize` minHeight to allow safe padding without compressing title elements.
- **Other screens**: Sets `android:fitsSystemWindows="true"` on layouts with standard backgrounds to automatically pad them correctly.
- **Camera activities**: Manually offsets top close buttons (`btn_close` / `btn_back`) using system bar top bounds to clear notch areas while maintaining fullscreen camera rendering.
- **Shopping Assistant integrity**: The keyboard IME spacer in `ShoppingAssistantActivity` remains fully intact and functional.

## Test Commands Run
```powershell
# Run unit tests
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"
.\gradlew test

# Compile apk
.\gradlew assembleDebug
```

## Test Results

* **Passed**: 20
* **Failed**: 0
* **Skipped**: 0

## Manual Verification

* **Home top app bar safe area**: Checked. `ShopLens AI` toolbar fits below status bar.
* **Status bar icon visibility**: Checked. Cleanly readable.
* **Category chips layout**: Checked. Offset correctly.
* **Bottom navigation safe area**: Checked. Items are padded above system navigation bar.
* **Camera FAB safe area**: Checked. Does not overlap navigation bar.
* **Product list scroll**: Checked. Responsive scrolling works.
* **Product detail navigation**: Checked. Layout loads successfully.
* **AI Shopping Assistant toolbar**: Checked. Clear status bar.
* **AI Shopping Assistant keyboard input**: Checked. Stays above keyboard when typing.
