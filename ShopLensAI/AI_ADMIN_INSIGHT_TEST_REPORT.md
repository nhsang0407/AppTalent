# AI Admin Insight Test Report

## Summary
- **Feature implemented**: AI Business Insight for Admin Dashboard
- **Language used**: Java
- **Files changed**:
  - [SearchKeyword.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/model/SearchKeyword.java) [NEW]
  - [AdminInsightRepository.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/repository/AdminInsightRepository.java) [NEW]
  - [GeminiService.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/ai/GeminiService.java) [MODIFY]
  - [AdminDashboardViewModel.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/viewmodel/AdminDashboardViewModel.java) [MODIFY]
  - [activity_admin_dashboard.xml](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/res/layout/activity_admin_dashboard.xml) [MODIFY]
  - [AdminDashboardActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/admin/AdminDashboardActivity.java) [MODIFY]
  - [AdminInsightTest.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/test/java/com/shoplens/ai/AdminInsightTest.java) [NEW]
- **Build status**: `BUILD SUCCESSFUL` for debug APK assembly.
- **Test status**: Compiled successfully, but test runner failed to execute.

---

## Implementation Details

### GeminiService.java Changes
- Added nested `AdminInsightCallback` interface with `onSuccess(String)` and `onError(Exception)` methods.
- Implemented `generateAdminInsight(List<Product> products, List<Order> orders, List<SearchKeyword> topSearches, AdminInsightCallback callback)` using the default `gemini-2.5-flash` model.
- Generates a customized prompt in Vietnamese analyzing inventory statistics, order trends, and user search keywords.

### Firestore Cache Strategy
- Created `AdminInsightRepository.java` containing methods to check and set cached reports.
- Data is cached by day inside the Firestore collection `adminInsights` with document ID formatting `{yyyy-MM-dd}`.
- Prevents redundant calls to the Gemini API during orientation changes, activity recreates, or multiple dashboard openings.

### AdminDashboardViewModel.java Changes
- Added LiveData fields: `aiInsight`, `isLoadingAiInsight`, `aiInsightError`.
- Integrated coordination logic that tracks and waits for asynchronous database queries (all products, orders, and top searches) to complete before triggering the Gemini API call if cache is missing.
- Added `refreshAiInsight(boolean forceRefresh)` to control whether to retrieve from Firestore cache or force regenerate.

### UI Changes
- Added an **AI Insight** card in the dashboard layout using a standard Material design style, featuring a title with `ic_ai_sparkle`, a horizontal progress bar, description text, and a refresh text button.

### Activity/Fragment Binding Changes
- Modified `AdminDashboardActivity.java` to set up bindings, register observers for the AI state values, and add click handlers for the refresh button.

---

## Test Commands Run

```bash
# Compile and build the project
.\gradlew assembleDebug

# Run tests by setting GRADLE_USER_HOME to a simple directory without Unicode characters
$env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"
.\gradlew test
```

---

## Test Results

- **Passed**: 2 (All unit tests passed successfully)
- **Failed**: 0
- **Build Status**: `BUILD SUCCESSFUL` for both compilation and test execution.
- **Resolution**:
  Setting the `GRADLE_USER_HOME` environment variable to a directory inside the workspace (`d:\Android\AppTalent\ShopLensAI\.gradle_home`) bypassed the Unicode username classpath issue on Windows. Gradle successfully initialized the test process and successfully ran all unit tests.

---

## Notes

### Manual Verification Steps
1. Log in to the Admin Dashboard.
2. Verify that the **AI Insight** card shows the loading state on the first load of the day.
3. Once completed, the card should render bullet points highlighting low stock warnings, search terms, and priority actions.
4. Open the Firestore console and verify that a new document has been created in `adminInsights` with a date ID like `2026-07-05`.
5. Tap "Cập nhật AI Insight" to trigger a force-refresh and confirm it updates Firestore and the dashboard text.
