# Implementation Plan: Tổng hợp phản hồi theo nhân viên

## Architecture decisions

- Mở rộng payload `filterRestful` bằng summary preview tùy chọn, không chèn entity giả.
- Tạo service tổng hợp riêng để tính khoảng thời gian, truy vấn, nhóm response và gắn
  ưu tiên lịch sử.
- GET xem tổng hợp nằm dưới `/home/employee/**`; employee bị ép dùng principal của mình.
- Hai POST ghi dữ liệu nằm dưới `/home/manager/**`.
- Tái sử dụng logic service của sửa deadline/xóa mềm; không gọi controller trong vòng lặp.
- Không migration database vì bảng ưu tiên hiện đã lưu `priority_date`.

## Phases

1. Viết test cho khoảng thời gian và truy vấn ưu tiên lịch sử.
2. Xây DTO/service tổng hợp và preview.
3. Tích hợp dòng tổng hợp vào trang private và trang chi tiết responsive.
4. Viết test rồi triển khai bulk deadline transaction.
5. Viết test rồi triển khai bulk soft-delete transaction.
6. Chạy test tích hợp phân quyền/UI, full test, build JAR và cập nhật PROJECT.md.

## Risks and mitigations

- ID bị sửa trên client: server tính lại tập ID hợp lệ theo account/period.
- Batch cập nhật dở dang: khóa bản ghi và dùng một transaction.
- Công việc Cả phòng ảnh hưởng toàn bộ người dùng: render disabled và backend từ chối.
- Mất ưu tiên lịch sử khi đổi người phụ trách: chỉ xóa ưu tiên ngày hiện tại trong luồng
  đổi người phụ trách; xóa hẳn mới dọn toàn bộ lịch sử.
- N+1 query: một query response có join event, một query attachment theo danh sách response,
  một query priority theo selection.

## Verification checkpoints

- Sau service: unit tests thời gian, nhóm response và priority pass.
- Sau API/UI: MockMvc kiểm tra employee/manager và nội dung template pass.
- Sau bulk actions: test rollback, Cả phòng, deadline session/day pass.
- Cuối cùng: `gradlew.bat --no-daemon test bootJar` pass. Không dùng `clean` vì
  thư mục `build` đang chứa JAR/source backup cần giữ nguyên.
