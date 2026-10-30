# Cấu hình tích hợp tra giá

## 1. Google Apps Script

1. Đồng bộ nội dung `WebApp.gs` trong thư mục `Hệ thống tra giá` lên project Apps Script tra giá trung tâm.
2. Mở **Project Settings > Script Properties** và thêm:
   - Tên: `PVT_PRICE_SHARED_TOKEN`
   - Giá trị: một chuỗi bí mật dài, ngẫu nhiên.
3. Vào **Deploy > Manage deployments**, sửa deployment Web App đang dùng và chọn **New version**.
4. Web App phải chạy với quyền của chủ sở hữu. Máy chủ PVT phải gọi được URL `/exec` mà không cần đăng nhập Google.
5. Giữ lại URL `/exec` của deployment; cập nhật phiên bản không làm đổi URL này.

## 2. PVT

Khai báo trong file `application.properties` thực tế của máy chủ:

```properties
app.price-lookup.enabled=true
app.price-lookup.web-app-url=https://script.google.com/macros/s/DEPLOYMENT_ID/exec
app.price-lookup.shared-token=CHUOI_BI_MAT_GIONG_APPS_SCRIPT
app.price-lookup.connect-timeout-seconds=10
app.price-lookup.read-timeout-seconds=330
```

Khởi động lại PVT. Hibernate sẽ tự tạo bốn bảng `price_lookup_*` do project đang dùng
`spring.jpa.hibernate.ddl-auto=update`.

## 3. Định dạng nhập

Mỗi dòng gồm keyword, phím Tab (hoặc ít nhất hai dấu cách), rồi đến đúng `accountName`
của người dùng PVT:

```text
Tăng đơ cáp vải bản rộng 100mm, mã CH10T-15M100, ADTECH    DTRAMTT
Găng*tay*hàn    NV02
```

PVT tự chia danh sách thành các request tối đa 20 hạng mục cho Apps Script. Một lần nhập tối đa
100 hạng mục; mỗi hạng mục được chọn tối đa 12 tên vật tư.
