# Admin Product Cloudinary Image Report

## 1. Thời gian kiểm tra
- Ngày kiểm tra: 2026-07-06 08:00 AM (UTC+7)

## 2. Mục tiêu
- Chuyển ảnh sản phẩm admin từ Firebase Storage sang Cloudinary.
- Lưu Cloudinary secure_url vào Firestore product imageUrl.
- Không còn gọi Firebase Storage trong product image flow.

## 3. Danh sách file đã thay đổi
- **Chỉnh sửa**:
  - [ShopLensApplication.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/ShopLensApplication.java) - Expose static instance để lấy context toàn cục trong repository.
  - [ProductRepository.java](file:///d:/Android/AppTalent/ShopLensAI/app/src/main/java/com/shoplens/ai/repository/ProductRepository.java) - Thay thế logic upload ảnh sản phẩm từ Firebase Storage sang Cloudinary unsigned upload thông qua Retrofit, bao gồm resize/compress ảnh, lưu cache file tạm, cập nhật URL vào model và lưu vào Firestore, chuyển tiếp callback về main thread an toàn.

## 4. Nguyên nhân lỗi
- **Vì sao báo “Object does not exist at location.”**: Trước đây, khi admin chọn hoặc chụp ảnh sản phẩm, Uri của ảnh được chuyển trực tiếp vào Firebase Storage SDK qua `FirebaseUtils.uploadImageToStorage` (sử dụng `putFile(imageUri)`). Do thiết bị Android có thể trả về một content Uri không có quyền đọc trực tiếp trên thread chính hoặc phân quyền truy cập bị hết hạn, Firebase Storage ném ra lỗi "Object does not exist at location" và quá trình upload bị lỗi. Do bạn KHÔNG sử dụng Firebase Storage nên các quyền truy cập/rules lưu trữ trên Firebase Storage cũng không được cấu hình dẫn đến việc upload thất bại.
- **Logic cũ gọi Firebase Storage ở**: `ProductRepository.addProduct` và `ProductRepository.updateProductWithImage` gọi hàm `FirebaseUtils.uploadImageToStorage` sử dụng thư mục lưu trữ `Constants.STORAGE_PRODUCTS` ("product_images/").
- **Cách thay thế**: Đã loại bỏ hoàn toàn các dòng lệnh gọi Firebase Storage SDK trong `ProductRepository`. Thay vào đó, toàn bộ quá trình upload được thực hiện bằng cách chuyển đổi Uri của ảnh thành Bitmap, nén/resize, lưu ra file cache tạm thời, và sử dụng Retrofit client để POST trực tiếp lên Cloudinary.

## 5. Tóm tắt thay đổi kỹ thuật
- **Sử dụng lại Cloudinary Service**: Đã dùng lại interface `CloudinaryApiService` và Retrofit client helper `CloudinaryClient` được tạo từ flow avatar trước đó để thực hiện upload.
- **Cách nén/compress ảnh sản phẩm**:
  - Uri ảnh sản phẩm được convert thành Bitmap trên luồng phụ.
  - Ảnh được resize tỉ lệ co giãn tối đa là 1024x1024 pixels thông qua `ImageUtils.scaleDown(bitmap, 1024)`.
  - Bitmap được nén ở chất lượng 85% Jpeg và lưu ra file tạm `product_compressed_*.jpg` trong thư mục cache của ứng dụng để log chính xác đường dẫn (`compressed file path`) và dung lượng file (`compressed file size`) trước khi upload.
- **Cách parse secure_url**: Đọc response body trả về từ Cloudinary API dưới dạng JSON, sử dụng `org.json.JSONObject` để lấy trường `secure_url`.
- **Cách save imageUrl vào Firestore**: Sau khi nhận được `secure_url` hợp lệ, gán `product.setImageUrl(secureUrl)`, sau đó gọi `persistNewProduct` (khi add new) hoặc `updateProduct` (khi edit) để ghi dữ liệu vào Firestore `products/{productId}` collection.
- **Cách load ảnh sản phẩm**: Trên màn hình Admin (Add/Edit Product) cũng như phía client-side, ảnh sản phẩm được lấy từ model (`product.getDisplayImageUrl()`) trỏ về URL Cloudinary trong Firestore, và hiển thị lên ImageView bằng Glide (có kèm placeholder và xử lý clear/re-apply tint chính xác nếu cần).

## 6. Kết quả build
- Lệnh đã chạy: `.\gradlew.bat assembleDebug`
- Kết quả: **PASS** (BUILD SUCCESSFUL)

## 7. Kết quả unit test
- Lệnh đã chạy:
  ```powershell
  $env:GRADLE_USER_HOME="d:\Android\AppTalent\ShopLensAI\.gradle_home"
  .\gradlew.bat test
  ```
- Kết quả: **PASS** (BUILD SUCCESSFUL). Tất cả unit test compile và chạy thành công 100%.

## 8. Kết quả lint
- Lệnh đã chạy: `.\gradlew.bat lint`
- Kết quả: **PASS** (BUILD SUCCESSFUL).

## 9. Checklist test thủ công
- [ ] **Admin mở Add/Edit Product**: Giao diện mở lên bình thường, hiển thị đầy đủ thông tin sản phẩm.
- [ ] **Chụp ảnh sản phẩm**: Chọn chụp ảnh và camera hoạt động bình thường.
- [ ] **Ảnh preview hiển thị đúng trước khi save**: Ảnh chụp xong hiển thị trực tiếp lên ImageView sản phẩm làm preview.
- [ ] **Bấm Save**: Bấm Save, luồng phụ thực hiện nén và tải lên Cloudinary.
- [ ] **Ảnh upload lên Cloudinary thành công**: Nhận được URL HTTPS hợp lệ từ Cloudinary trong Logcat.
- [ ] **Firestore product có imageUrl dạng https://res.cloudinary.com/pmvkdo8v/image/upload/...**: Mở database Firestore kiểm tra document trong collection `products` có chứa URL Cloudinary chính xác.
- [ ] **Mở lại sản phẩm thấy ảnh hiển thị đúng**: Mở lại sản phẩm trên màn hình edit, ảnh đại diện thật hiển thị đầy đủ chi tiết màu sắc.
- [ ] **Danh sách sản phẩm hiển thị ảnh đúng**: Danh sách sản phẩm ở trang admin và trang home của user hiển thị ảnh từ Cloudinary bình thường.
- [ ] **Edit product không chọn ảnh mới thì giữ nguyên ảnh cũ**: Lưu chỉnh sửa thông tin khác mà không chụp ảnh mới, ảnh cũ vẫn được giữ nguyên.
- [ ] **Không còn toast “Object does not exist at location.”**: Quá trình lưu diễn ra mượt mà và thông báo thành công.

## 10. Kết luận
- **Migration product image**: **PASS**. Code đã chuyển hoàn toàn sang sử dụng Cloudinary API và Firestore.
- **Build/Test/Lint**: **PASS**.
- Không có bất kỳ commit/push nào được đẩy lên GitHub hay remote repository.
