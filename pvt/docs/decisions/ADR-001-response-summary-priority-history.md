# ADR-001: Tổng hợp phản hồi dùng dữ liệu ưu tiên lịch sử hiện có

## Trạng thái

Đã chấp nhận.

## Ngày

2026-07-31.

## Bối cảnh

Quản lý cần xem nhanh các phản hồi của từng nhân viên theo hôm nay, hôm trước,
buổi sáng hoặc buổi chiều và đối chiếu với thứ tự ưu tiên của đúng ngày đó. Dữ
liệu ưu tiên hiện đã được tách theo `daily_priority_selection.priority_date` và
`task_priority`; sang ngày mới ứng dụng chỉ đọc một ngày khác chứ không xóa bản
ghi cũ.

Tính năng cũng cần rollback JAR an toàn vì ứng dụng đang chạy ổn trên máy chủ
nội bộ. Thay đổi schema chỉ để phục vụ một view tổng hợp sẽ làm rollback phức tạp
không cần thiết.

## Quyết định

- Không tạo bảng hoặc cột database mới cho bản tổng hợp.
- Tổng hợp phản hồi truy vấn trực tiếp `response_event_information` theo
  `createdByUser` và khoảng thời gian, đồng thời chỉ lấy công việc chưa ẩn/chưa xóa.
- Ưu tiên được đọc theo ngày tương ứng của khoảng tổng hợp. Hôm trước dùng đúng
  bản ghi ưu tiên hôm trước; hôm nay/sáng/chiều dùng ngày hiện tại.
- Khi đổi người phụ trách, chỉ xóa ưu tiên của công việc trong ngày hiện tại. Ưu
  tiên lịch sử được giữ để báo cáo hôm trước không mất ngữ cảnh.
- Khi xóa cứng công việc hoặc tài khoản, vẫn xóa toàn bộ ưu tiên liên quan theo
  luồng dọn dữ liệu hiện có.
- Các thao tác deadline/xóa nhiều mục dùng transaction chung, kiểm tra toàn bộ
  danh sách rồi mới ghi. Công việc Cả phòng chỉ được xem, không được chọn.

## Phương án đã cân nhắc

### Tạo bảng snapshot tổng hợp

Có thể lưu nội dung công việc, phản hồi và ưu tiên thành snapshot mỗi ngày. Cách
này truy vấn nhanh nhưng tăng schema, cần job tạo snapshot và tạo thêm rủi ro dữ
liệu thiếu khi máy chủ tắt ban đêm. Không chọn.

### Xóa ưu tiên mỗi ngày rồi lưu bản sao riêng

Làm tăng dữ liệu và logic đồng bộ trong khi bảng hiện tại đã có `priority_date`.
Không chọn.

## Hệ quả

- JAR mới chạy trên database hiện tại, không có migration mới.
- Rollback về JAR cũ không phải rollback database.
- Lịch sử ưu tiên chỉ tồn tại khi không xóa cứng công việc/tài khoản, phù hợp với
  vòng đời dữ liệu hiện tại.
- Bản tổng hợp phản ánh đúng nội dung phản hồi gốc và file đính kèm; không dùng AI
  hoặc dịch vụ ngoài nên không phát sinh chi phí hay rò rỉ dữ liệu nội bộ.
