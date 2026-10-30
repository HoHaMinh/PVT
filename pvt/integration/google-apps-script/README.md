# Kết nối cảnh báo công nợ Google Drive với PVT

## Nguyên tắc an toàn

Phần tích hợp này **không sửa và không gọi** `masterCheckAllFilesAndSendEmail()`.
Trigger Gmail 7–8 giờ sáng tiếp tục chạy như trước. File
`PVT_WebApp.gs` là phần bổ sung chỉ đọc Google Sheets và trả JSON khi
PVT chủ động yêu cầu; trong file không có lệnh `MailApp.sendEmail()`.

PVT gọi ra Google qua HTTPS nên không cần mở port mới, không cần thêm route
Cloudflare Tunnel và vẫn dùng được khi Apps Script chạy trước lúc máy chủ PVT bật.

## 1. Chuẩn bị quyền Google Drive

Tài khoản Google dùng để sở hữu/deploy Apps Script phải có quyền **Viewer trở
lên** đối với:

- File master có sheet `Danh sach nhan vien` và `Lich nghi le`.
- Tất cả file Google Sheets công nợ của nhân viên.

Nếu các file nhân viên cùng nằm trong một thư mục Drive, có thể chia sẻ cả thư
mục cho tài khoản này; file con sẽ kế thừa quyền. Không cần cấp quyền Drive cho
máy chủ Windows hoặc cho từng tài khoản PVT.

## 2. Bổ sung Bravo ID vào sheet master

Trong sheet `Danh sach nhan vien`, giữ nguyên ba cột cũ và thêm cột D:

| Cột | Nội dung |
|---|---|
| A | Họ tên nhân viên |
| B | ID hoặc URL Google Sheets công nợ |
| C | Email nhận cảnh báo — hàm Gmail cũ vẫn dùng |
| D | Bravo ID, chính là `account.account_name` trong database PVT |

Dữ liệu bắt đầu từ hàng 4. Ví dụ D4 là `dminhhh`. Giá trị phải khớp tuyệt đối
với account đăng nhập PVT; cột này không phải họ tên hiển thị.

Hàm Gmail cũ chỉ đọc A:C nên việc thêm D không làm thay đổi chức năng cũ.

## 3. Thêm mã vào Apps Script

1. Mở file master → **Extensions** → **Apps Script**.
2. Không sửa file/hàm Gmail cũ.
3. Mở file `PVT_WebApp.gs` hiện có; nếu chưa có thì bấm dấu `+` → **Script** và
   đặt tên `PVT_WebApp`.
4. Thay toàn bộ nội dung bằng file `PVT_WebApp.gs` trong thư mục tích hợp này rồi
   Save. Không giữ hai bản có các hàm `doPost()` trùng tên trong cùng project.
5. Vào **Project Settings** → **Script properties** → Add script property:
   - `PVT_MASTER_SPREADSHEET_ID`: chuỗi nằm giữa `/d/` và `/edit` trong URL file master.
   - `PVT_SHARED_TOKEN`: chuỗi bí mật dài, không dùng mật khẩu Gmail/MySQL.

Có thể tạo token trong PowerShell:

```powershell
$token = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$token
```

Không gửi ảnh chụp hoặc commit token vào Git.

## 4. Cấp quyền lần đầu và kiểm tra logic

1. Trong danh sách function, chọn `pvtTestDebtAlertCollection`.
2. Bấm **Run**.
3. Chấp nhận quyền đọc Google Sheets theo hướng dẫn của Google.
4. Mở **Execution log** và kiểm tra JSON có `"success": true`.

Hàm test chỉ đọc dữ liệu và ghi log, không gửi email.

## 5. Deploy Web App

1. **Deploy** → **New deployment**.
2. Type: **Web app**.
3. Execute as: **Me** (tài khoản có quyền xem các file Drive).
4. Who has access: **Anyone**.
5. Deploy, cấp quyền nếu được hỏi và copy URL kết thúc bằng `/exec`.

Endpoint bắt buộc để `Anyone` vì ứng dụng Java 8 không đăng nhập Google; dữ liệu
vẫn được bảo vệ bằng `PVT_SHARED_TOKEN`. Nếu tài khoản Google Workspace chặn lựa
chọn này, quản trị Workspace phải cho phép hoặc cần chuyển sang phương án OAuth
khác — không đổi sang “execute as user accessing” vì PVT không có phiên Google.

Khi sửa mã Apps Script sau này: **Deploy** → **Manage deployments** → Edit →
chọn **New version** → Deploy. URL `/exec` giữ nguyên.

## 6. Cấu hình PVT trên máy chủ

Thêm vào file `application.properties` nằm **cùng thư mục với `pvt.jar`**:

```properties
app.debt-alert.enabled=true
app.debt-alert.web-app-url=https://script.google.com/macros/s/DEPLOYMENT_ID/exec
app.debt-alert.shared-token=TOKEN_GIONG_SCRIPT_PROPERTY
app.debt-alert.connect-timeout-seconds=10
app.debt-alert.read-timeout-seconds=330
app.debt-alert.startup-delay-ms=3000
app.debt-alert.retry-delay-ms=900000
app.debt-alert.earliest-sync-hour=7
app.debt-alert.task-deadline-hour=16
app.public-url=https://othpdn.phongvattu.online
```

Hai token phải giống tuyệt đối. Không thêm dấu nháy quanh URL/token. Mỗi site có
thể dùng file master và Web App riêng; tuyệt đối không copy token vào source JAR.

Nếu chưa sẵn sàng dùng tính năng, để:

```properties
app.debt-alert.enabled=false
```

Khi tắt, menu/trang Home không hiện và PVT không gọi Google.

## 7. Cách chạy và kiểm tra

1. Bật MySQL.
2. Khởi động `pvt.jar` như bình thường.
3. Sau khoảng 3 giây, lần chạy đầu tiên luôn gọi Apps Script và đồng bộ lịch.
4. Nếu thất bại, PVT lưu trạng thái lỗi và thử lại mỗi 15 phút; đây chỉ là retry,
   không phải thời gian chờ của lượt khởi động.
5. Thành công hoặc ngày nghỉ/Chủ Nhật thì không quét lại định kỳ trong ngày.
6. Manager có thể mở **Cảnh báo công nợ** → **Quét lại Google Drive** bất kỳ lúc nào.

Hibernate tự tạo năm bảng:

- `debt_alert_sync`: trạng thái từng ngày.
- `debt_alert_employee`: kết quả từng nhân viên.
- `debt_alert_item`: các hồ sơ đến hạn/sắp đến hạn.
- `debt_import_alert_item`: snapshot hồ sơ NK đã bước vào 10 ngày cảnh báo.
- `debt_import_alert_ack`: xác nhận hồ sơ đã báo kế toán; dữ liệu này được giữ
  qua các lần quét và qua ngày mới.

Với mỗi account có cảnh báo, PVT tạo một công việc cá nhân deadline 16:00 hôm
đó. Account không có hóa đơn cảnh báo sẽ không được tạo lịch. Quét lại cùng ngày
cập nhật công việc hiện có, không tạo bản trùng.

Với hồ sơ có cột O là `NK` và cột M còn phải thanh toán lớn hơn 0, Web App tính
hạn sớm hơn giữa cột P + 90 ngày và cột Q + 45 ngày. Từ 10 ngày trước hạn, PVT
hiện cảnh báo đỏ. Employee xem được nội dung cảnh báo của chính mình nhưng chỉ
manager thấy và sử dụng nút **Đã báo kế toán**. Xác nhận xong cảnh báo không hiện
lại dù hồ sơ vẫn chưa thanh toán hết.

Kiểm tra log:

```powershell
Select-String -Path .\logs\pvt.log -Pattern "công nợ|Apps Script" | Select-Object -Last 30
```

Có thể thử endpoint trực tiếp từ máy chủ (không chụp màn hình có token):

```powershell
$url = 'https://script.google.com/macros/s/DEPLOYMENT_ID/exec'
$token = 'TOKEN_GIONG_SCRIPT_PROPERTY'
$body = @{ action = 'scanDebtAlerts'; token = $token } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri $url -ContentType 'application/json' -Body $body
```

## 8. Phân quyền hiển thị

- Employee chỉ thấy kết quả có cột D trùng account đang đăng nhập.
- Manager thấy toàn bộ nhân viên và lỗi cấu hình/mapping.
- Hàng thiếu cột D hoặc ID không có trong PVT không làm sập trang; manager thấy
  trạng thái `MISSING_ACCOUNT` hoặc `ACCOUNT_NOT_FOUND` để sửa.
- PVT không nhận dữ liệu qua Cloudflare và không tạo API công khai ở domain PVT.

## 9. Không ảnh hưởng Gmail cũ

Sau khi deploy, kiểm tra trigger trong Apps Script vẫn trỏ tới đúng
`masterCheckAllFilesAndSendEmail`. Không tạo trigger cho `doPost` hoặc
`pvtTestDebtAlertCollection`; Web App tự gọi `doPost` khi PVT gửi HTTP POST.

Nếu muốn rollback riêng tính năng này, đặt `app.debt-alert.enabled=false` rồi
restart PVT. Trigger Gmail cũ vẫn hoạt động kể cả khi PVT tắt.
