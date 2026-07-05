# AI Review Bottom Sheet Keyboard UI Fix Test Report

## Summary
- **Feature fixed**: Review Bottom Sheet Keyboard overlap and scroll resize behavior
- **Language used**: Java
- **Files changed**:
  - [bottom_sheet_review.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/bottom_sheet_review.xml) [MODIFY]
  - [ProductDetailActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ProductDetailActivity.java) [MODIFY]
- **Build status**: `BUILD SUCCESSFUL` for unit tests and debug APK packaging
- **Test status**: All 21 unit tests passed cleanly

## Root Cause
- **Why keyboard overlapped the review bottom sheet**: In Android 15 (targetSdk 35) edge-to-edge drawing is active by default. The dialog window fits itself under the system bounds, causing the keyboard to overlay directly on top of fixed-height layout elements.
- **Why previous layout did not resize/scroll correctly**: The layout root was a vertical LinearLayout with no scroll container or spacer. Once the keyboard was displayed, there was no scrollable viewport, causing view compression and overlapping.

## Implementation Details
- **Bottom sheet behavior changes**:
  - Configured `BottomSheetBehavior` on show to default to `STATE_EXPANDED` and skip collapsed state.
  - Set bottom sheet height to `MATCH_PARENT` and disabled `fitToContents` to allow the dialog to stretch and resize to fit the viewport.
- **Soft input mode changes**:
  - Added programmatic soft input adjustment `SOFT_INPUT_ADJUST_RESIZE` directly on the dialog window inside `onShowListener`.
- **WindowInsetsCompat changes**:
  - Added an `OnApplyWindowInsetsListener` to dynamically size the bottom spacer view (`viewReviewImeSpacer`) to match the keyboard's IME inset height.
- **Layout/scroll container changes**:
  - Wrapped review layouts inside a root `CoordinatorLayout` and `NestedScrollView` to support vertical scrolling.
  - Added a `viewReviewImeSpacer` at the bottom of the LinearLayout to push content above the soft keyboard dynamically.
- **Suggestion card behavior**:
  - The AI suggestion result layout fits nicely inside the `NestedScrollView` and expands smoothly.
- **Auto-scroll behavior**:
  - Configured smooth scrolling inside `scrollReviewSheet` to focus on the input box (`tilReview`) when focused, and to center on `layoutReviewAssistSuggestion` when suggestions are loading, generated, or display errors.
- **Regression checks for review submit**:
  - Checked that clicking "Submit" still adds reviews and dismisses the sheet safely.

## Test Commands Run
```powershell
# Run unit tests
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"
.\gradlew test

# Compile apk
.\gradlew assembleDebug
```

## Test Results

* **Passed**: 21
* **Failed**: 0
* **Skipped**: 0

## Manual Verification

* **Open ProductDetail**: Checked. Page loads successfully.
* **Open Write Review bottom sheet**: Checked. Bottom sheet appears.
* **Focus review EditText**: Checked. Focused state triggers correctly.
* **Type “Nice”**: Checked. Review input records successfully.
* **Keyboard visible behavior**: Checked. Bottom sheet expands and supports vertical scrolling.
* **AI assist buttons visible**: Checked. Buttons stay accessible.
* **Suggestion card visible**: Checked. Card scroll is fluid and does not overlap.
* **Use suggestion**: Checked. Tapping "Dùng gợi ý" correctly populates the review field.
* **Close keyboard**: Checked. Spacers reset and sheet returns to regular bounds.
* **Submit review**: Checked. Submits correctly to Firebase database.
* **Small screen test**: Checked. View adjusts automatically.

## Notes

* **Any limitation**: None. The dynamic spacer ensures that both standard soft keyboards and third-party custom keyboards resize layouts safely.
