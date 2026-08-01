# Spec: Tổng hợp phản hồi theo nhân viên

## Objective

Giúp quản lý xem trong một trang toàn bộ phản hồi của một nhân viên theo mốc thời gian,
sau đó đổi deadline hoặc xóa mềm nhiều công việc cùng lúc. Nhân viên cũng được xem phần
tổng hợp của chính mình nhưng không được thực hiện thao tác quản lý.

## Approved behavior

- Dòng tổng hợp là dữ liệu hiển thị ảo, không tạo `MonthEvent` trong database.
- Dòng tổng hợp đứng STT 1 ở tab nhân viên cụ thể; không xuất hiện ở tab `Tất cả`.
- Cả `ROLE_EMPLOYEE` và `ROLE_MANAGER` được xem trang tổng hợp.
- Employee chỉ được tổng hợp tài khoản đang đăng nhập và chỉ thấy nút trở lại.
- Manager thấy checkbox, `Đổi deadline nhiều mục`, `Quản lý xóa nhiều mục` và nút trở lại.
- Mốc mặc định là hôm nay; các mốc khác là hôm trước, từ sáng đến giờ, từ chiều đến giờ.
- Mốc sáng là `[00:00, min(now, 12:00))`; mốc chiều là `[12:00, now]`.
- Chỉ lấy phản hồi do account đang tổng hợp gửi, xét theo `createdByDate`.
- Không lấy công việc ẩn hoặc đã xóa mềm. Công việc Cả phòng được đọc nhưng không được
  chọn cho thao tác hàng loạt.
- Ưu tiên hiển thị theo `priority_date` tương ứng với ngày được tổng hợp. Hôm nay/sáng/chiều
  dùng ưu tiên hôm nay; hôm trước dùng ưu tiên hôm trước.
- Không có ưu tiên thì không hiện tiền tố. Có ưu tiên thì hiện `(N)` màu đỏ trước nội dung.
- Phản hồi chỉ có attachment phải hiện thông tin attachment thay vì một dòng trống.

## Time ranges

- `TODAY`: 00:00 hôm nay đến thời điểm truy vấn.
- `YESTERDAY`: 00:00 hôm trước đến trước 00:00 hôm nay.
- `MORNING`: 00:00 đến thời điểm truy vấn nếu trước 12:00; nếu đã qua trưa thì đến trước 12:00.
- `AFTERNOON`: không có dữ liệu trước 12:00; từ 12:00 đến thời điểm truy vấn nếu đã qua trưa.
- Tất cả phép tính dùng `Clock` với múi giờ `Asia/Ho_Chi_Minh`.

## Bulk deadline

- Một lựa chọn áp dụng cho toàn bộ công việc được check.
- `ONE_DAY`, `TWO_DAYS`, `THREE_DAYS` cộng ngày lịch và giữ nguyên giờ/phút.
- `ONE_SESSION`: deadline trước 12:00 thành 16:00 cùng ngày; deadline từ 12:00 trở đi
  thành 11:00 ngày kế tiếp.
- Không tự bỏ qua Chủ nhật.
- Hiển thị preview deadline cũ và mới trước khi xác nhận.
- Cập nhật deadline và log `Báo cáo lại ...` phải thành công toàn bộ hoặc rollback toàn bộ.

## Bulk delete

- Chỉ xóa mềm: đặt `is_deleted=true`, giữ nguyên response và attachment.
- Toàn bộ ID phải hợp lệ trước khi ghi; một ID lỗi làm rollback cả batch.
- Server kiểm tra lại role, account, period và danh sách công việc đủ điều kiện.
- Chỉ phát một tín hiệu SSE sau khi batch thành công.

## Boundaries

- Không thêm bảng/cột database cho phần tổng hợp.
- Không dùng AI/API bên ngoài; giữ nguyên nội dung phản hồi.
- Không cho client gửi ID tùy ý để sửa/xóa ngoài kết quả tổng hợp hiện tại.
- Không thay đổi luồng chat, ưu tiên hôm nay, thùng rác hay sửa một công việc ngoài phần
  tách helper dùng chung cần thiết.

## Success criteria

- Employee không thể xem tổng hợp người khác và không gọi được route manager.
- Manager xem đúng phản hồi/ưu tiên theo cả bốn mốc.
- 64 phản hồi được tải bằng truy vấn theo khoảng thời gian, không truy vấn từng công việc.
- Bulk deadline/delete là transaction all-or-nothing.
- Công việc Cả phòng không thể chọn hàng loạt.
- UI hoạt động ở 320px, 768px, 1024px và 1440px.
- Toàn bộ test và Gradle build thành công; JAR mới được xuất cùng bản rollback.
