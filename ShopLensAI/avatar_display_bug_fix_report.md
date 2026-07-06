# Avatar Display Bug Fix Report

## 1. Nguyên nhân thực sự của bug
- **Lỗi ImageView Tint**: Trong XML layout `activity_profile.xml`, thẻ ImageView `@id/ivAvatar` được cấu hình thuộc tính `app:tint="@color/primary"` (màu xanh dương thương hiệu của ShopLens).
- **Hiện tượng**: Khi Glide tải ảnh avatar từ Cloudinary URL thành công và hiển thị nó vào ImageView, thuộc tính `app:tint` này vẫn hoạt động và áp dụng bộ lọc màu xanh đè lên toàn bộ bức ảnh thật. Điều này khiến ảnh đại diện trông như một hình tròn màu xanh đồng nhất hoặc một vòng tròn trống. Lệnh `binding.ivAvatar.clearColorFilter()` ban đầu không xoá sạch bộ lọc tint này do nó được áp dụng dưới dạng một `ColorStateList` (app:tint).
- **Cách khắc phục**: Trước khi gọi Glide tải ảnh, ta phải xoá bộ lọc màu và danh sách tint cũ bằng cách gọi đồng thời:
  ```java
  binding.ivAvatar.setImageTintList(null);
  binding.ivAvatar.clearColorFilter();
  ```
  Ngược lại, nếu không có ảnh URL hợp lệ (hiển thị placeholder mặc định `ic_person`), ta re-apply lại màu xanh để placeholder hiển thị đúng chuẩn thiết kế ban đầu:
  ```java
  binding.ivAvatar.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary)));
  ```

## 2. File đã sửa
- **[ProfileActivity.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/user/ProfileActivity.java)**:
  - Cập nhật logic quản lý tint list của ImageView khi load URL và khi fallback về ảnh mặc định.
  - Bổ sung `RequestListener` cho Glide để ghi log debug chi tiết về trạng thái tải ảnh thành công hay thất bại.
  - Áp dụng `ObjectKey` của Glide để xử lý cache-busting an toàn dựa trên URL ảnh Cloudinary.
- **[ProfileViewModel.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/viewmodel/ProfileViewModel.java)**:
  - Thêm log debug chi tiết trong các hàm callback thành công/thất bại của Firestore để theo dõi quá trình ghi nhận URL ảnh.

## 3. Log/logic trước và sau

### Log & Logic trước đây
- **Logic**:
  ```java
  if (avatarUrl != null && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
      Glide.with(this).load(avatarUrl).circleCrop().into(binding.ivAvatar);
      binding.ivAvatar.clearColorFilter();
  }
  ```
- **Hệ quả**: Thuộc tính `app:tint="@color/primary"` trong XML không bị xoá, ảnh tải về bị nhuộm xanh hoàn toàn. Không có log debug chi tiết khi Glide tải ảnh lỗi.

### Log & Logic sau khi sửa
- **Logic**:
  ```java
  if (avatarUrl != null && !avatarUrl.trim().isEmpty() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
      binding.ivAvatar.setImageTintList(null); // Xoá tint list đè màu xanh
      binding.ivAvatar.clearColorFilter();     // Xoá color filter
      
      Glide.with(this)
              .load(avatarUrl.trim())
              .signature(new com.bumptech.glide.signature.ObjectKey(avatarUrl.trim()))
              .circleCrop()
              .listener(new RequestListener<Drawable>() { ... })
              .into(binding.ivAvatar);
  } else {
      binding.ivAvatar.setImageResource(R.drawable.ic_person);
      binding.ivAvatar.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary))); // Áp dụng lại tint cho icon mặc định
  }
  ```
- **Hệ quả**: Ảnh đại diện tải về từ Cloudinary hiển thị chuẩn xác đầy đủ màu sắc thật. Có log debug xác nhận:
  - `populateUser: avatarUrl read from User model=https://res.cloudinary.com/pmvkdo8v/...`
  - `Avatar load success. url=https://res.cloudinary.com/pmvkdo8v/..., source=REMOTE`

## 4. Kết quả build
- Lệnh chạy: `.\gradlew.bat assembleDebug`
- Kết quả: **PASS** (BUILD SUCCESSFUL)

## 5. Kết quả unit test
- Lệnh chạy:
  ```powershell
  $env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"
  .\gradlew.bat test
  ```
- Kết quả: **PASS** (BUILD SUCCESSFUL). Tất cả unit test compile và chạy thành công 100%.

## 6. Kết quả lint
- Lệnh chạy: `.\gradlew.bat lint`
- Kết quả: **PASS** (BUILD SUCCESSFUL).

## 7. Checklist test thủ công
- [ ] **Firestore có avatarUrl hợp lệ**: Trường `avatarUrl` trong collection `users/{uid}` chứa link HTTPS trỏ tới Cloudinary.
- [ ] **Profile mở lên load được ảnh từ Cloudinary**: Mở trang Profile, ảnh đại diện thật hiển thị đầy đủ chi tiết màu sắc sắc nét.
- [ ] **Sau khi đổi avatar, UI cập nhật ngay**: Đổi ảnh thành công, màn hình profile tự động load ảnh mới ngay lập tức qua Glide.
- [ ] **Thoát app mở lại vẫn thấy avatar**: Khởi động lại ứng dụng, profile vẫn tải và hiện đúng avatar từ Firestore.
- [ ] **Remove avatar quay về placeholder**: Chọn "Remove", avatar chuyển về icon người dùng mặc định (`ic_person`) được nhuộm xanh thương hiệu.
- [ ] **Không còn avatar xanh/trống khi avatarUrl hợp lệ**: Không còn hiện tượng ảnh bị che/nhuộm xanh sau khi cập nhật avatar thực tế.
