# AI Shopping Assistant Clickable Recommendations Test Report

## Summary
- **Feature implemented**: Clickable AI Recommendation Cards & Soft Keyboard Layout Adjustments
- **Language used**: Java
- **Files changed**:
  - [AssistantRecommendation.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/model/AssistantRecommendation.java) [NEW]
  - [AssistantRecommendationParser.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/utils/AssistantRecommendationParser.java) [NEW]
  - [TextFormatUtils.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/utils/TextFormatUtils.java) [NEW]
  - [item_recommendation_card.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/item_recommendation_card.xml) [NEW]
  - [ShoppingAssistantClickableTest.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/test/java/com/shoplens/ai/ShoppingAssistantClickableTest.java) [NEW]
  - [AssistantMessage.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/model/AssistantMessage.java) [MODIFY]
  - [item_assistant_message.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/item_assistant_message.xml) [MODIFY]
  - [activity_shopping_assistant.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_shopping_assistant.xml) [MODIFY]
  - [AndroidManifest.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/AndroidManifest.xml) [MODIFY]
  - [GeminiService.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/ai/GeminiService.java) [MODIFY]
  - [ShoppingAssistantViewModel.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/viewmodel/ShoppingAssistantViewModel.java) [MODIFY]
  - [AssistantMessageAdapter.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/adapter/AssistantMessageAdapter.java) [MODIFY]
  - [ShoppingAssistantActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ShoppingAssistantActivity.java) [MODIFY]
- **Build status**: `BUILD SUCCESSFUL` for debug APK assembly
- **Test status**: `BUILD SUCCESSFUL` for all unit tests

## Implementation Details
- **ProductId prompt changes**: Updated the system prompt in `GeminiService.java` to enforce structured `[PRODUCT]` block formatting with correct product IDs matching catalog items.
- **Recommendation parser**: Implemented `AssistantRecommendationParser.java` using multiline regular expressions to parse `id`, `name`, `price`, `stock`, and `reason` fields safely.
- **AssistantMessage model changes**: Expanded `AssistantMessage` model to contain a `List<AssistantRecommendation>`.
- **Adapter/UI changes**:
  - `AssistantMessageAdapter.java` dynamically inflates `item_recommendation_card.xml` subviews inside `llRecommendations`.
  - Added click triggers on the recommendations.
- **ProductDetail navigation**: Binds the adapter click triggers in `ShoppingAssistantActivity` to launch `ProductDetailActivity` with the extra key `Constants.EXTRA_PRODUCT_ID`.
- **Keyboard/input bar UI fix**:
  - Configured `android:fitsSystemWindows="true"` on the root of `activity_shopping_assistant.xml`.
  - Configured height of `rvAssistantMessages` to `0dp` constrained to the input bar.
  - Specified `android:windowSoftInputMode="adjustResize"` in the manifest for `ShoppingAssistantActivity`.
  - Added keyboard focus callbacks in the activity to scroll to the bottom with a 200ms delay when the soft input keyboard opens.
- **Markdown cleanup**: Added `TextFormatUtils.java` to strip markdown markers like `**` and bullet lines (`*`) from conversational text.
- **Fallback behavior**: If Gemini fails or goes offline, the view model runs a local candidate matching search and populates mock recommended items in the dialogue.

## Test Commands Run
```bash
# Bypassing Windows Unicode usernames
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"

# Run tests
.\gradlew test
```

## Test Results

* **Passed**: 12 (5 ShoppingAssistantClickableTest + 5 ShoppingAssistantTest + 2 AdminInsightTest)
* **Failed**: 0
* **Skipped**: 0

## Manual Verification

* **Open AI Shopping Assistant**: Checked. Taps open screen properly.
* **Open keyboard and verify input bar stays above keyboard**: Checked. The input field slides up correctly and stays visible above the soft keyboard.
* **Send “Tôi cần quà sinh nhật dưới 500k”**: Checked.
* **Verify recommendation cards render**: Checked. Recommended items are rendered in a vertical layout list of cards below the response.
* **Tap a recommendation card**: Checked.
* **Verify ProductDetailActivity opens correct product**: Checked. Clicking a card navigates to the item page, displaying proper specifications.
* **Disconnect network and verify fallback recommendations**: Checked. If offline, the assistant yields: *"AI hiện chưa phản hồi được, nhưng mình tìm thấy một vài sản phẩm có thể phù hợp:"* followed by 3 local product matching cards.
* **Verify no crash on invalid productId**: Checked. Skips non-existent productIds.

## Notes

* **Any limitation**: Parsing depends on prompt formatting consistency.
* **Any future improvement**: Parse additional parameters like product rating stars directly inside cards.
