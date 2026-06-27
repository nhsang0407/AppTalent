# 🛒 ShopLens AI

An AI-powered retail management & shopping Android app built in **Java** with **Firebase**,
**Google ML Kit**, **CameraX**, and the **Gemini Developer API** (via Firebase AI Logic).

It ships two experiences from one codebase, chosen by the signed-in user's role:

- **User** — browse products, visual + barcode search, cart, checkout, order history, reviews
  with AI review summaries.
- **Admin** — dashboard with revenue chart, product management, AI image & description
  generation, barcode-based stock updates, order management, and visual-search trend analytics.

The UI follows a deliberate design system (deep-blue `#1565C0` primary, amber `#FF6F00` accent,
Material 3 components, an 8-point spacing grid, opacity-based text hierarchy, soft shadows and
rounded cards) for a polished, production-feeling result.

---

## 1. Requirements

- Android Studio Ladybug (or newer) with **JDK 17**
- Android Gradle Plugin 8.2.2 / Gradle 8.7 (wrapper config included)
- `compileSdk 35`, `minSdk 26`, `targetSdk 35`
- A physical device or emulator with a camera (for barcode/visual search)

> The wrapper JAR is not bundled. Open the project in Android Studio once and it will generate
> `gradle/wrapper/gradle-wrapper.jar` automatically, or run `gradle wrapper` if you have Gradle installed.

---

## 2. Firebase setup (do this first — the app will not build without `google-services.json`)

1. Go to <https://console.firebase.google.com> → **Create project** → name it `ShopLensAI`.
2. **Authentication** → Sign-in method → **Email/Password** → Enable.
3. **Cloud Firestore** → Create database → Start in **test mode** → region `asia-southeast1`
   (recommended for Vietnam).
4. **Storage** → Get started → Start in **test mode**.
5. **Register an Android app**:
   - Package name: `com.shoplens.ai`
   - Download `google-services.json` and place it in the **`app/`** folder.
     (A `app/google-services.json.template` is included to show the expected shape — replace it
     with your real file; do not just rename the template.)
6. **Enable Firebase AI Logic (Gemini Developer API)**:
   - In the Firebase console open the **AI Logic** (a.k.a. *Build with Gemini / AI*) section and
     enable it. This provisions a Gemini Developer API key tied to your project — **no separate
     API key is needed in the app**. The code uses
     `FirebaseAI.getInstance(GenerativeBackend.googleAI())`.
7. Paste the **Firestore security rules** below (Firestore → Rules → Publish).

### Firestore security rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    match /products/{productId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    match /orders/{orderId} {
      allow read, write: if request.auth != null &&
        (resource.data.userId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin');
      allow create: if request.auth != null;
    }
    match /reviews/{reviewId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    match /searchLogs/{logId} {
      allow create: if request.auth != null;
      allow read: if request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

> Note: review submission also writes aggregate `averageRating`/`reviewCount` onto the product
> document inside a transaction. The product `write` rule is admin-only, so **reviews by normal
> users will be blocked from updating the product aggregates** under these exact rules. If you want
> users' reviews to update product rating, relax the `products` rule, e.g.:
> ```
> match /products/{productId} {
>   allow read: if request.auth != null;
>   allow create, delete: if isAdmin();
>   allow update: if request.auth != null; // allow rating/summary updates
> }
> ```
> (or scope the allowed fields). Keep the stricter rule if you don't need user-driven rating updates.

---

## 3. Build & run

1. Open the project in Android Studio (**File → Open** → select the `ShopLensAI` folder).
2. Let Gradle sync. If `firebase-ai` fails to resolve, bump the Firebase BOM in
   `app/build.gradle` to the latest (`com.google.firebase:firebase-bom:34.+`).
3. Run on a device/emulator.

---

## 4. Test accounts

Create these through the app's **Register** screen (everyone registers as `role = "user"`):

| Role  | Email                | Password   | How it becomes admin |
|-------|----------------------|------------|----------------------|
| Admin | `admin@shoplens.ai`  | `admin123` | After registering, open Firestore → `users` → that user's document → set field `role` = `admin` (string). Re-login. |
| User  | `user@shoplens.ai`   | `user123`  | Nothing — defaults to `user`. |

To make an admin:
1. Register the account in the app.
2. Firebase console → Firestore → `users` collection → open the new document.
3. Edit field `role` from `user` to `admin` (type: string). Save.
4. Log out and log back in → you land on the **Admin Dashboard**.

Seed a few products as admin (Products → ＋), then browse/checkout as the user account.

---

## 5. Architecture

```
MVVM, ViewBinding everywhere, callbacks/listeners for all Firebase I/O.

ui (Activities)  ──observes──>  ViewModel (LiveData)  ──>  Repository  ──>  Firebase / ML Kit / Gemini
```

- `model/` — Firestore POJOs with `@PropertyName` + no-arg constructors.
- `repository/` — all data access (`Auth/Product/Order/Review/SearchLog` + in-memory `CartManager`).
- `viewmodel/` — `AndroidViewModel`s exposing `MutableLiveData` state.
- `ai/` — `MLKitService` (barcode/object/label/smart-reply) and `GeminiService`
  (visual search, review summary, description & image generation).
- `user/`, `admin/`, `auth/` — screens. `MainActivity` is a pure role router.
- The cart is a process-wide singleton (`CartManager`) so it is shared across activities and is
  intentionally **not** persisted across app restarts.

---

## 6. Known limitations (free Firebase / Spark plan)

- **Cloud Storage on Spark**: newer projects may require the Blaze plan to use Cloud Storage.
  If image upload fails, either upgrade to Blaze (pay-as-you-go, generous free tier) or skip
  product photos.
- **Gemini image generation** uses a preview model id (`gemini-2.5-flash-image-preview`) in
  `GeminiService`. Preview model names change; if image generation errors, set `IMAGE_MODEL` to the
  current image-capable model enabled in your AI Logic console. Text features use `gemini-2.5-flash`.
- **Firestore search** is client-side filtered (Firestore has no full-text search). Fine for a
  demo-sized catalog; use Algolia/Typesense for large catalogs.
- **No composite indexes required** — queries use single-field filters and sort client-side.
- **Gemini free tier** has rate limits; rapid AI calls may be throttled.
- Reviews updating product rating requires the relaxed `products` rule noted above.

---

## 7. Project map

- `app/src/main/java/com/shoplens/ai/` — all Java sources (48 files)
- `app/src/main/res/layout/` — 25 layouts
- `app/src/main/res/drawable/` — icons, shapes, launcher (vector)
- `app/src/main/res/values/` — `colors`, `strings`, `themes`, `dimens`, `arrays`

Built with the **mobile-app-ui-design** skill design principles applied to native Android XML.
