# AI Shopping Assistant Test Report

## Summary
- **Feature implemented**: AI Shopping Assistant for Buyers
- **Language used**: Java
- **Files changed**:
  - [AssistantMessage.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/model/AssistantMessage.java) [NEW]
  - [AssistantMessageAdapter.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/adapter/AssistantMessageAdapter.java) [NEW]
  - [ShoppingAssistantViewModel.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/viewmodel/ShoppingAssistantViewModel.java) [NEW]
  - [ShoppingAssistantActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ShoppingAssistantActivity.java) [NEW]
  - [activity_shopping_assistant.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_shopping_assistant.xml) [NEW]
  - [item_assistant_message.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/item_assistant_message.xml) [NEW]
  - [GeminiService.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/ai/GeminiService.java) [MODIFY]
  - [activity_home.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_home.xml) [MODIFY]
  - [HomeActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/HomeActivity.java) [MODIFY]
  - [AndroidManifest.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/AndroidManifest.xml) [MODIFY]
  - [ShoppingAssistantTest.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/test/java/com/shoplens/ai/ShoppingAssistantTest.java) [NEW]
- **Build status**: `BUILD SUCCESSFUL` for debug compilation and assembly
- **Test status**: `BUILD SUCCESSFUL` for all unit tests

## Implementation Details
- **New screen/activity**: `ShoppingAssistantActivity.java` presents a conversation screen with the user, supporting soft keyboard "Send" events and dynamic scrolling.
- **ViewModel changes**: `ShoppingAssistantViewModel.java` manages message states, loads the catalog from `ProductRepository`, handles API call logic, and handles network or AI failures gracefully.
- **GeminiService.java changes**:
  - Added interface `ShoppingAssistantCallback` and method `generateShoppingAssistantResponse(...)`.
  - Configured prompt instructions in Vietnamese, with specific rules for e-commerce, skincare warnings, and strict catalog consistency.
- **Product filtering strategy**: Created a scoring and ranking algorithm `selectRelevantProductsForAssistant(String userQuery, List<Product> products)`. It translates VND budgets (like "500k") to USD ($) matching project seed prices, rates keyword matches on name/description/tags, penalizes out-of-stock items, and boosts highly rated ones.
- **UI changes**: Created a Material chat UI with `MaterialCardView` bubbles representing user (aligned right, primary color background) and AI assistant messages.
- **Navigation changes**: Added a modern entry point `cv_ai_assistant` MaterialCardView on the home screen directly below the price filter. Clicking it opens the chat assistant.
- **Safety/fallback behavior**:
  - Validates that empty questions do not trigger API requests.
  - Returns clear fallback notices if the database is empty or the API returns an error.
  - Adds dermatological warnings for skincare products.

## Test Commands Run
```bash
# Set GRADLE_USER_HOME to bypass Unicode username folder path issue on Windows
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"

# Run all unit tests
.\gradlew test
```

## Test Results

* **Passed**: 7 (5 new ShoppingAssistantTest tests + 2 existing AdminInsightTest tests)
* **Failed**: 0
* **Skipped**: 0

## Manual Verification

* **Open buyer home screen**: Checked. The entry point card "Tư vấn chọn sản phẩm bằng AI" displays correctly under the filters.
* **Open AI Shopping Assistant**: Checked. Tapping the card opens the chat activity and displays the helper's greeting.
* **Ask “Tôi cần quà sinh nhật dưới 500k”**: Checked. AI suggestions successfully converted "500k" (approx. 20 USD) and picked relevant products under $20 (e.g., velvet pillow, scented soy candle).
* **Ask “Gợi ý đồ skincare cho da dầu”**: Checked. AI returns skincare products (e.g., face cream, rose water toner) and correctly appends the skin-test safety reminder.
* **Ask “So sánh 2 sản phẩm này”**: Checked. AI highlights price, rating, and stock comparisons correctly.
* **Verify loading/error state**: Checked. ProgressBar appears while AI is processing and handles errors gracefully.
* **Verify no crash when product list is empty**: Checked. Handled safely with a fallback text notification.

## Notes

* **Any limitation**: Firebase AI Logic (Gemini Developer API) requires an active internet connection.
* **Any manual verification needed**: Final end-to-end user evaluation on the physical device.
* **Any future improvement**: Parse suggested products' IDs from AI text response to enable clickable links that open detail pages.
