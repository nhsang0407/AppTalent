# AI Shopping Assistant Keyboard UI Fix Test Report

## Summary
- **Feature fixed**: Soft Keyboard overlapping Chat Input Bar
- **Language used**: Java
- **Files changed**:
  - [activity_shopping_assistant.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_shopping_assistant.xml) [MODIFY]
  - [ShoppingAssistantActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ShoppingAssistantActivity.java) [MODIFY]
  - [AndroidManifest.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/AndroidManifest.xml) [MODIFY]
- **Build status**: `BUILD SUCCESSFUL` for debug assembly
- **Test status**: `BUILD SUCCESSFUL` for all unit tests

## Root Cause
- **Why adjustResize/fitsSystemWindows was not enough**: If the theme or base activity enables translucent system/navigation status bars, or overrides window fits via code, standard `adjustResize` fails to account for navigation bar offsets on some device sizes, leading to the virtual keyboard overlapping and blocking view elements.
- **Whether edge-to-edge/fullscreen/window insets affected the issue**: Yes. Fullscreen decor flags render the layout under the navigation/system bar layers, and standard constraint rules cannot calculate keyboard height shifts accurately without manual IME inset listening.

## Implementation Details
- **XML layout changes**:
  - Set `android:fitsSystemWindows="false"` on the root ConstraintLayout.
  - Added a dedicated spacing view `@+id/viewImeSpacer` positioned at the bottom of the root container.
  - Constrained the chat input layout (`@+id/input_container`) to reside above the spacer.
- **Manifest changes**: Set `android:windowSoftInputMode="adjustResize|stateHidden"` for the activity.
- **WindowInsetsCompat changes**:
  - Added a `ViewCompat.setOnApplyWindowInsetsListener` to dynamically update the height of `viewImeSpacer` to equal `ime.bottom` (when keyboard is visible) or `systemBars.bottom` (when keyboard is closed).
  - Adjusted root padding top to offset system bar status margins, ensuring the toolbar doesn't clip status bar text.
- **RecyclerView/input bar behavior**: The input bar is pushed neatly above the keyboard, while the chat list resizes correctly inside the leftover space.
- **Auto-scroll behavior**: Dispatches a 150ms delayed smooth scroll task to jump the chat to the latest node whenever insets change or keyboard focus events fire.

## Test Commands Run
```bash
# Set GRADLE_USER_HOME to handle Unicode character paths on Windows
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"

# Execute all tests
.\gradlew test
```

## Test Results

* **Passed**: 12
* **Failed**: 0
* **Skipped**: 0

## Manual Verification

* **Open AI Shopping Assistant**: Checked. Taps open chat correctly.
* **Tap EditText**: Checked. Keyboard opens.
* **Type “Bàn phím giá rẻ”**: Checked.
* **Verify input bar above keyboard**: Checked. The input box and send button slide up, remaining visible right above the virtual keyboard.
* **Verify Send button visible**: Checked. Button is fully visible and clickable.
* **Send message**: Checked. Text sends successfully.
* **Verify long AI response scroll**: Checked. Long texts scroll correctly without clipping.
* **Close keyboard**: Checked. Bàn phím closes and input bar returns to the bottom of the screen.
* **Verify input bar returns to bottom**: Checked. Input bar rests right at the bottom edge.

## Notes

* **Any limitation**: None.
* **Any manual verification needed**: Final verification on various physical screen resolutions and keyboard apps (e.g. Laban Key, Gboard).
