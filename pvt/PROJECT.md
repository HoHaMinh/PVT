# PVT — PROJECT KNOWLEDGE BASE

> Tài liệu ngữ cảnh kỹ thuật và nghiệp vụ.
> AI phải đọc toàn bộ file này trước khi phân tích hoặc sửa project.

## 1. Mục đích dự án

PVT là ứng dụng web quản lý công việc nội bộ của Phòng Vật tư Ống thép Hòa Phát Đà Nẵng.

Đối tượng sử dụng:

- Quản lý phòng.
- Nhân viên trong phòng.

Đây không phải hệ thống thương mại, không phải multi-tenant và không chứa dữ liệu đặc biệt nhạy cảm.

Thứ tự ưu tiên:

1. Trải nghiệm người dùng mượt và dễ hiểu.
2. Hoạt động ổn định trên PC và mobile.
3. Cập nhật dữ liệu gần realtime.
4. Dễ sửa và không làm hỏng dữ liệu hiện tại.
5. Bảo mật ở mức cơ bản: đăng nhập và phân quyền quản lý/nhân viên.

Không tự ý bổ sung kiến trúc hoặc bảo mật doanh nghiệp phức tạp nếu chưa được yêu cầu.

## 2. Công nghệ

- Java 8.
- Gradle Wrapper 7.6.
- Spring Boot 2.7.9.
- Spring MVC.
- Spring Security.
- Spring Data JPA/Hibernate.
- MySQL.
- Thymeleaf.
- Bootstrap 5.2.3.
- jQuery 4.0.0.
- Server-Sent Events.
- Flatpickr.
- SweetAlert.
- SortableJS.

Dependency được khai báo trong `build.gradle`.

## 3. Cấu trúc chính
```text
pvt/
├── build.gradle
├── settings.gradle
├── PROJECT.md
├── src/main/java/com/hoaphat/pvt/
│   ├── PvtApplication.java
│   ├── config/
│   ├── controller/
│   ├── model/
│   ├── repository/
│   ├── service/
│   ├── formatter/
│   └── util/
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   └── static/
└── src/test/
```
Các file repomix-output.xml, repomix-structure.xml và repomix-templates.xml chỉ là snapshot tự động. Không sửa chúng thay cho source gốc.
static/src/ chủ yếu chứa source Bootstrap/MDB đóng gói sẵn. Khi sửa UI, ưu tiên đọc template, home.css và JavaScript tự viết.
## 4. Cách ứng dụng khởi động
Điểm vào:
src/main/java/com/hoaphat/pvt/PvtApplication.java
Luồng khởi động:
Đặt encoding UTF-8.
Đặt java.awt.headless=false.
Khởi động Spring Boot và embedded Tomcat.
Kết nối MySQL.
Hibernate tự cập nhật schema bằng ddl-auto=update.
Bật JPA auditing.
Bật scheduler.
Tạo icon Windows System Tray nếu được hỗ trợ.
Nhấp đúp icon mở http://localhost.
Menu icon cho phép tắt server.
Ứng dụng được thiết kế để chạy trên một máy Windows nội bộ.
## 5. Cấu hình runtime
File:
src/main/resources/application.properties
Cấu hình hiện tại:
Database: MySQL localhost:3306/pvt.
Có thể tự tạo database nếu chưa tồn tại.
Hibernate dùng ddl-auto=update.
SQL được in ra log.
Server chạy port 80.
Ảnh: E:/IT/images.
Video: E:/IT/videos.
Có cấu hình forwarded headers cho Cloudflare/reverse proxy.
Session cookie đặt secure=true.
Chưa có profile dev/test/prod riêng.
Username/password database đang được đặt trực tiếp trong file cấu hình. Không ghi lại giá trị password trong tài liệu này.
## 6. Kiến trúc request
Browser
  -> Spring Security / HTTP Session
  -> Controller
  -> Service
  -> Repository
  -> MySQL
Controller có thể:
Trả Thymeleaf template.
Trả JSON bằng ResponseEntity.
Mở kết nối SSE.
Đây là monolith server-rendered, không phải SPA.
## 7. Đăng nhập và session
Luồng đăng nhập:
Mở / hoặc /login.
Form POST tới /doLogin.
Username field có tên accountname.
UserDetailsServiceImpl đọc Account.
Role được đọc từ AccountRole.
Password được so sánh bằng BCrypt.
Đăng nhập thành công redirect /home.
Session chứa:
accountName: ID đăng nhập, ví dụ ho.ha.minh.
username: họ tên hiển thị, ví dụ Hồ Hà Minh.
accountName là định danh ổn định. username chỉ nên dùng để hiển thị.
## 8. Phân quyền
File:
config/SecurityConfig.java
Quy tắc:
/home/employee/**: ROLE_EMPLOYEE hoặc ROLE_MANAGER.
/home/manager/**: chỉ ROLE_MANAGER.
/, /login, CSS và JS: không cần đăng nhập.
Các URL khác yêu cầu đăng nhập.
Access denied chuyển tới /deny.
Logout dùng POST /logout.
Phân quyền trang quản lý hiện hoạt động dựa trên role.
Quyết định đã thống nhất
Không ưu tiên khóa việc nhân viên chủ động xem nội dung công việc của nhau.
Tuy nhiên UI không nên khiến nhân viên thao tác nhầm việc của người khác.
Phản hồi của quản lý phải được xác định bằng ROLE_MANAGER.
Không được khóa cứng username quản lý.
## 9. Mô hình dữ liệu
Account
File:
model/account/Account.java
Field:
accountName: primary key, ID đăng nhập.
password: BCrypt hash.
name: họ tên.
Role
File:
model/account/Role.java
Field:
roleId.
roleName.
Role thường dùng:
ROLE_EMPLOYEE
ROLE_MANAGER
AccountRole
Bảng liên kết account với role:
id.
account.
role.
MonthEvent
File:
model/event/MonthEvent.java
Đây là entity công việc chính.
Field:
monthEventId: ID.
monthEventDescription: nội dung.
monthEventDeadline: deadline.
monthEventStatus: loại việc.
responseStatus: loại phản hồi gần nhất.
extendDay: số ngày lặp.
lastTimeResponse: thời gian phản hồi gần nhất.
lastPersonResponse: account phản hồi gần nhất.
registeredDay: ngày giao việc.
account: người phụ trách.
all: giao cả phòng.
hidden: trạng thái ẩn.
Ý nghĩa:
Field	Giá trị	Ý nghĩa
monthEventStatus	0	Việc thông thường
monthEventStatus	1	Việc tháng theo luồng cũ
monthEventStatus	2	Việc lặp lại
responseStatus	0	Chưa phản hồi
responseStatus	1	Quản lý phản hồi
responseStatus	2	Nhân viên phản hồi
all	true	Giao cả phòng
all	false	Giao cá nhân
hidden	true	Đang ẩn

lastTimeResponse mặc định dùng mốc:
00:00 01-01-2023
Giao diện xem mốc này là “Chưa có phản hồi”.
ResponseEventInformation
Field:
idResponse.
eventInformationResponse.
monthEvent.
createdByUser.
createdByDate.
Nội dung phản hồi liên kết với một MonthEvent.
UserTabOrder
Dù nằm trong package dto, đây là JPA entity.
Field:
managerUsername.
accountName.
displayOrder.
Hiện managerUsername lấy từ họ tên hiển thị trong session, không phải account ID.
SpecialMessage
Dùng để hiển thị thông báo trên trang chủ khi ngày/tháng của birthday trùng ngày hiện tại.
Nếu có nhiều record cùng ngày, service hiện chỉ trả record đầu tiên.
SercuritySchedule
Tên class đang viết sai chính tả nhưng được dùng nhất quán.
Entity chứa:
str1.
str2.
str3.
str4.
Dùng để hiển thị khối nhắc nhở trên trang lịch.
## 10. Controller và endpoint
LoginController
/
/login
/logout
/deny
HomeController
GET /home
Hiển thị:
Header.
Navbar.
Thông báo đặc biệt.
Banner hoặc welcome screen mobile.
AccountController
Base path:
/home/manager
Endpoint:
GET  /create-account
POST /create-account
EventController
Endpoint chung/nhân viên:
GET  /home/employee/private
GET  /home/employee/filterRestful
POST /home/employee/private/addEventResponse
GET  /home/employee/private/showEventResponseForm/{id}
GET  /home/employee/private/showEventResponseFormRestful/{id}
GET  /home/employee/notifications/subscribe
Endpoint quản lý:
GET  /home/manager/task
GET  /home/manager/task/showAddPrivateEventForm
GET  /home/manager/task/showAddMonthEventForm
POST /home/manager/task/addMonthEvent

GET  /home/manager/task/showEditMonthEventForm/{id}
POST /home/manager/task/editMonthEvent

GET  /home/manager/task/hide/{id}
GET  /home/manager/task/show/{id}
POST /home/manager/task/toggleHidden/{id}

GET  /home/manager/weeklyTask
GET  /home/manager/weeklyTask/showAddWeekEventForm
POST /home/manager/weeklyTask/addWeekEvent
GET  /home/manager/weeklyTask/delete/{id}

GET  /home/manager/delete-account
POST /home/manager/delete-account

POST /home/manager/saveTabOrder
Sau các thao tác thay đổi dữ liệu, controller thường gọi:
notificationService.sendRefreshSignal();
ImageController
GET /home/employee/image
GET /home/employee/video
Controller quét subfolder và tạo danh sách media.
## 11. Luồng giao việc
Việc thông thường
Quản lý mở trang task.
JavaScript dùng prompt hỏi số lượng việc.
Controller tạo danh sách MonthEvent rỗng.
Form hiển thị nhiều dòng.
Quản lý nhập nội dung, người phụ trách và deadline.
Form gửi MonthEventManager.
Service đặt registeredDay=now.
Nếu chọn ALL, đặt all=true.
Nếu giao cá nhân, liên kết Account.
Lưu từng event.
Phát SSE refresh.
Định hướng UX:
Bỏ prompt.
Thêm/xóa dòng trực tiếp trong form.
Không phải tải lại trang khi thay đổi số lượng việc.
Sửa việc
Quản lý có thể sửa:
Nội dung.
Deadline.
Người phụ trách.
Sau khi sửa, controller tạo một phản hồi:
Báo cáo lại HH:mm dd/MM/yyyy
Hiện createdByDate của log này được đặt bằng deadline mới, không phải thời điểm sửa.
Controller đang bắt toàn bộ exception rồi redirect mà không báo nguyên nhân. Khi cải tiến phải hiển thị lỗi thân thiện.
Ẩn/hiện
Hiện có hai cơ chế:
GET /hide/{id} và /show/{id}.
POST /toggleHidden/{id}.
Nên thống nhất UX trong tương lai.
Xóa việc
deleteWeekEvent(id):
Xóa response liên quan.
Xóa event.
Method này được dùng cho cả việc thường và việc lặp lại dù tên chứa WeekEvent.
## 12. Trang lịch và realtime
Template:
templates/private.html
Luồng:
Controller lấy account và role.
Nhân viên chỉ thấy tab của mình trên UI.
Quản lý thấy tab “Tất cả” và các nhân viên.
Quản lý có thể kéo đổi thứ tự tab.
JavaScript gọi /filterRestful.
JSON được dựng thành các row trong bảng.
Trang mở EventSource.
Khi nhận SSE refresh, gọi lại API.
Khoảng thời gian truy vấn:
730 ngày trước.
30 ngày sau.
Màu hiện tại:
Deadline còn không quá 60 phút: row cảnh báo.
responseStatus=0: nút xám.
responseStatus=1: nút xanh lá.
responseStatus=2: nút vàng.
Phản hồi mới gần đây có màu nhắc.
Khi cải tiến realtime:
Không làm mất tab đang chọn.
Không làm mất nội dung người dùng đang nhập.
Không refresh toàn trang.
Có loading nhẹ.
Không làm bảng nhấp nháy.
Cho phép EventSource tự reconnect.
## 13. Phản hồi công việc
Template:
templates/eventResponse.html
Trang gồm:
Nội dung công việc.
Timeline phản hồi.
Modal thêm phản hồi.
Nút đổi deadline.
Nút xóa.
Nút ẩn/hiện.
Nút trở lại tab cũ.
Submit phản hồi:
Lấy user từ Principal.
Escape HTML nội dung.
Lưu ResponseEventInformation.
Cập nhật responseStatus.
Cập nhật người/thời gian phản hồi cuối.
Phát SSE.
Đóng modal.
Xóa textarea.
Tải lại timeline.
Biến isInteract được dùng để tránh tải lại khi người dùng đang gõ.
## 14. Việc lặp lại
Việc lặp lại dùng:
monthEventStatus = 2
extendDay = số ngày chu kỳ
Cơ chế hiện tại không phải scheduler nghiệp vụ thực sự.
checkWeekEventDeadline(now) chỉ chạy khi người dùng mở:
/home/employee/private
Logic:
Nếu now > deadline + 5 giờ:
    deadline mới = deadline cũ + extendDay ngày
Hệ quả:
Không ai mở trang thì deadline không đổi.
Trễ nhiều chu kỳ thì mỗi lần mở chỉ tiến một chu kỳ.
Không tạo event mới.
Không reset responseStatus.
Không tách response theo kỳ.
Định hướng đã thống nhất:
Việc lặp lại nên chạy nền.
Không phụ thuộc người dùng mở trang.
Trước khi sửa cần xác định có reset trạng thái mỗi kỳ hay không.
Không tự ý thay schema trước khi thống nhất nghiệp vụ.
## 15. SSE NotificationService
NotificationService giữ danh sách SseEmitter.
Các event:
refresh -> data "update"
ping    -> data "heartbeat"
Heartbeat chạy mỗi 20 giây để Cloudflare không ngắt kết nối im lặng.
Emitter được xóa khi:
Completion.
Timeout.
Error.
Gửi event thất bại.
SSE chỉ báo “có thay đổi”; client vẫn gọi API để lấy dữ liệu mới.
## 16. Ảnh và video
Resource mapping:
/img-local/** -> E:/IT/images/
/vid-local/** -> E:/IT/videos/
Cache public 30 ngày.
Cấu trúc:
E:/IT/images/
├── Album A/
│   ├── image1.jpg
│   └── image2.webp
└── Album B/

E:/IT/videos/
├── Album A/
│   ├── video1.mp4
│   └── video2.webm
└── Album B/
Extension ảnh:
jpg, jpeg, png, gif, webp, bmp
Extension video:
mp4, webm, ogg, mov
Trang ảnh
Có:
Tab album.
Grid động.
Lightbox.
Next/previous.
Zoom.
Drag.
Scroll wheel.
Pinch zoom mobile.
Trang video
Có:
Tab album.
Video card.
Lightbox.
Next/previous.
Tên file lấy từ URL.
Điểm cần sửa:
Thiếu CSS ẩn album không active.
MIME lightbox bị cố định là video/mp4.
Cần lazy loading tốt hơn khi nhiều video.
## 17. Danh sách template
Template	Chức năng
login.html	Đăng nhập
home.html	Trang chủ
fragments.html	Header/navbar
private.html	Lịch cần làm
eventResponse.html	Phản hồi
task.html	Quản lý việc thường
privateEventForm.html	Giao việc cá nhân
monthEventForm.html	Việc tháng cũ
monthEditEventForm.html	Sửa việc
weeklyTask.html	Danh sách việc lặp
weekEventForm.html	Tạo việc lặp
create-account.html	Tạo account
deleteAccount.html	Xóa account
images.html	Album ảnh
video.html	Album video
deny.html	Trang 403

Navbar hiện vẫn hiển thị menu quản lý cho nhân viên. Spring Security chặn URL nhưng định hướng UX là ẩn menu quản lý khỏi nhân viên.
## 18. Tài khoản
Tạo account
Kiểm tra username trùng.
BCrypt password.
Lưu account.
Tìm role.
Lưu AccountRole.
Database mới phải có sẵn:
ROLE_EMPLOYEE
ROLE_MANAGER
Project chưa có migration hoặc seed.
Xóa account
Không cho quản lý xóa chính mình.
Xóa AccountRole.
Xóa event của account.
Xóa account.
Cần lưu ý response của event và dữ liệu UserTabOrder còn sót.
## 19. Định hướng UX ưu tiên
Form giao việc động, không dùng prompt.
Việc lặp lại chạy nền.
Realtime không mất tab hoặc nội dung đang nhập.
Có loading và empty state.
Thông báo lỗi/thành công rõ ràng.
Ẩn menu quản lý khỏi nhân viên.
Thêm tìm kiếm/lọc công việc.
Phản hồi dạng timeline trực quan.
Sửa album video và MIME.
Chuẩn hóa màu trạng thái.
Tối ưu mobile.
Không rewrite toàn bộ project nếu có thể sửa nhỏ.
## 20. Mã và tài nguyên ít sử dụng
UserValidate đang rỗng.
DateFormatter chưa được dùng rõ ràng.
Jade4j có dependency nhưng không thấy dùng.
image.js, image.css, image.sass thuộc giao diện cũ.
Nhiều source MDB/Bootstrap không được gọi trực tiếp.
Các file Repomix lớn gây nhiễu tìm kiếm.
Không xóa hàng loạt nếu chưa kiểm tra toàn project.
## 21. Test và build
Test hiện có duy nhất:
@SpringBootTest
class PvtApplicationTests {
    @Test
    void contextLoads() {}
}
Chưa có test riêng cho:
Controller.
Service.
Repository.
Security.
SSE.
Việc lặp lại.
Giao diện.
Test không có database profile riêng và có thể dùng MySQL thật.
Build Windows:
.\gradlew.bat clean build
Chạy:
java -jar build\libs\pvt.jar
Artifact trong build/libs có thể cũ hơn source. Khi triển khai phải build lại.
## 22. Quy tắc dành cho AI
Trước khi sửa:
Đọc toàn bộ PROJECT.md.
Đọc file source trực tiếp liên quan.
Không sửa file Repomix.
Kiểm tra Git diff.
Không ghi đè thay đổi của người dùng.
Không tự ý đổi schema.
Giữ tương thích Java 8/Spring Boot 2.7.9.
Ưu tiên thay đổi nhỏ, dễ kiểm tra.
Nếu sửa UI, xem cả desktop và mobile.
Nếu sửa realtime, không làm mất dữ liệu đang nhập.
Nếu kiểm tra manager, dùng role.
Không dùng danh sách username khóa cứng.
Cập nhật file này khi có quyết định nghiệp vụ mới.
Sau khi sửa phải báo:
File đã thay đổi.
Hành vi trước và sau.
Cách kiểm tra.
Test/build đã chạy.
Lý do nếu chưa thể chạy test.
## 23. Lịch sử quyết định
Đây là web nội bộ.
Ưu tiên UX và độ ổn định.
Không triển khai bảo mật doanh nghiệp phức tạp.
Giữ đăng nhập và phân quyền URL.
Không ưu tiên cấm nhân viên xem nội dung công việc của nhau.
Nhận diện phản hồi quản lý bằng ROLE_MANAGER.
Không khóa cứng dminhhh, dmont hoặc username khác.
Không thay schema nếu chưa được yêu cầu.
Không rewrite toàn bộ project khi chưa cần thiết.

## 24. Triển khai online qua Cloudflare Tunnel

### 24.1. Phân tách máy phát triển và máy chủ

Project không chạy online trực tiếp từ máy đang chứa source code.

Mô hình thực tế:

- Máy phát triển:
  - Chứa source code.
  - Dùng để sửa code, kiểm tra và đóng gói JAR.
  - Không phải máy đang phục vụ website online.
  - Thay đổi source không ảnh hưởng website cho tới khi build và chép JAR mới sang máy chủ.
- Máy chủ Windows:
  - Chạy MySQL qua Laragon.
  - Chạy `pvt.jar` bằng Java.
  - Chạy `cloudflared agent`.
  - Chứa các thư mục media `E:/IT/images` và `E:/IT/videos`.

Không được giả định process hoặc service trên máy phát triển phản ánh trạng thái máy chủ. Khi chẩn đoán lỗi online phải kiểm tra đúng máy chủ Windows.

### 24.2. Địa chỉ public và Cloudflare Tunnel

Địa chỉ người dùng truy cập:

```text
https://othpdn.phongvattu.online
```

Cấu hình đã xác nhận:

| Thuộc tính | Giá trị |
|---|---|
| Tunnel | `PVT-SERVER` |
| Trạng thái khi kiểm tra | `Healthy` |
| Số replica/connector | `1` |
| Số published application | `1` |
| Public hostname | `othpdn.phongvattu.online` |
| Dịch vụ đích | `http://localhost:80` |
| Windows Service | `cloudflared agent` |
| Trạng thái service | `Running` |
| Startup Type | `Automatic` |

Tunnel dùng kết nối outbound từ máy chủ tới Cloudflare. Website không phụ thuộc vào việc mở trực tiếp port 80 ra Internet hoặc có public IP cố định.

Chỉ có một replica trên một máy chủ nên không có high availability. Nếu máy chủ, Internet hoặc `cloudflared agent` dừng, website online sẽ mất kết nối.

### 24.3. Luồng request online

```text
Trình duyệt người dùng
  -> HTTPS https://othpdn.phongvattu.online
  -> Cloudflare Edge
  -> Cloudflare Tunnel PVT-SERVER
  -> cloudflared agent trên máy chủ Windows
  -> HTTP http://localhost:80
  -> Spring Boot / embedded Tomcat
  -> Service / Repository
  -> MySQL localhost:3306 do Laragon chạy
```

Kết nối từ người dùng tới Cloudflare dùng HTTPS. Route nội bộ từ `cloudflared` tới Spring Boot dùng HTTP trên loopback `localhost:80`.

Các cấu hình liên quan:

```properties
server.port=80
server.forward-headers-strategy=framework
server.servlet.session.cookie.secure=true
```

Ý nghĩa:

- Spring Boot phải chiếm được port 80.
- Spring xử lý forwarded headers để nhận biết request ban đầu đi qua HTTPS/Cloudflare.
- Session cookie được thiết kế cho truy cập HTTPS bằng domain public.
- Truy cập trực tiếp `http://localhost` có thể không hoạt động giống domain public đối với cookie Secure.
- Khi kiểm tra đăng nhập nên ưu tiên URL public.
- Không đổi port Spring Boot hoặc service Tunnel riêng lẻ; phải đổi đồng bộ cả hai.

### 24.4. Xác thực và phân quyền

Mở domain bằng cửa sổ ẩn danh đi thẳng tới trang đăng nhập PVT.

Điều này xác nhận:

- Không có Cloudflare Access đứng trước ứng dụng.
- Cloudflare Tunnel chỉ cung cấp đường truyền tới PVT.
- Trang đăng nhập PVT có thể truy cập từ Internet.
- Đăng nhập và phân quyền do Spring Security xử lý.
- Quyền vẫn dựa trên `Account`, `AccountRole`, `ROLE_EMPLOYEE` và `ROLE_MANAGER`.
- Không được dựa vào Tunnel để bỏ kiểm tra đăng nhập hoặc phân quyền URL.

### 24.5. Thành phần chạy trên máy chủ

| Thành phần | Cách chạy | Port/vai trò |
|---|---|---|
| `cloudflared agent` | Windows Service, tự chạy cùng Windows | Kết nối tới Cloudflare |
| MySQL | Khởi động thủ công trong Laragon | `localhost:3306` |
| Spring Boot | Khởi động thủ công bằng `run.vbs` | `localhost:80` |

Laragon chỉ chạy MySQL ở port 3306. Không chạy Apache hoặc Nginx, nhờ đó không tranh chấp port 80 với embedded Tomcat.

Nếu một web server khác hoặc một bản PVT cũ đang giữ port 80, JAR mới sẽ không khởi động được.

### 24.6. Script khởi động run.vbs

`run.vbs` nằm cùng thư mục với `pvt.jar` trên máy chủ.

```vbscript
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strPath = fso.GetParentFolderName(WScript.ScriptFullName)

' Thêm tham số -Dfile.encoding=UTF-8 ngay sau javaw
WshShell.Run "javaw -Dfile.encoding=UTF-8 -jar """ & strPath & "\pvt.jar""", 0, False
```

Hành vi:

- Lấy thư mục chứa `run.vbs` làm thư mục triển khai.
- Tìm JAR có tên chính xác `pvt.jar` trong cùng thư mục.
- Dùng `javaw` nên ứng dụng chạy nền, không mở console.
- Truyền `-Dfile.encoding=UTF-8` ngay khi JVM khởi động.
- Tham số `0` chạy cửa sổ ẩn.
- Tham số `False` làm VBS kết thúc ngay sau khi tạo process Java.
- VBS không giám sát hoặc tự khởi động lại Java khi ứng dụng dừng.
- `javaw.exe` phải được cài và có trong biến môi trường `PATH`.
- Script không tự chạy cùng Windows; người vận hành phải nhấp thủ công.

Lưu ý:

- Không nhấp `run.vbs` nhiều lần khi chưa kiểm tra icon System Tray hoặc process Java.
- Bản chạy thứ hai sẽ cố chiếm lại port 80 và có thể thất bại.
- Vì dùng `javaw`, lỗi khởi động không xuất hiện trong terminal.
- Cấu hình hiện chưa khai báo file log riêng.
- Khi cần chẩn đoán, có thể tạm chạy:

```powershell
java -Dfile.encoding=UTF-8 -jar pvt.jar
```

Lệnh trên cần chạy tại Command Prompt/PowerShell trên máy chủ để xem log trực tiếp.

Không đưa password database, Tunnel token, API token hoặc private key vào `run.vbs` hay tài liệu.

### 24.7. Windows System Tray

Sau khi Spring Boot khởi động thành công, `PvtApplication` tạo icon `PVT Hòa Phát` nếu máy chủ hỗ trợ System Tray.

Icon cho phép:

- Nhấp đúp để mở `http://localhost`.
- Chọn `Tắt server` để dừng ứng dụng.
- Hiển thị thông báo khi server khởi động thành công.

Do session cookie đặt `secure=true`, mở `http://localhost` có thể không kiểm tra đăng nhập chính xác như URL HTTPS public.

### 24.8. Trình tự khởi động sau khi Windows reboot

Trình tự hiện tại là bán thủ công:

1. Windows khởi động.
2. `cloudflared agent` tự chạy vì Startup Type là `Automatic`.
3. Người vận hành mở Laragon.
4. Chỉ khởi động MySQL trên port 3306.
5. Chờ MySQL sẵn sàng.
6. Nhấp `run.vbs`.
7. `javaw` chạy `pvt.jar` nền.
8. Chờ icon hoặc thông báo PVT khởi động thành công.
9. Mở `https://othpdn.phongvattu.online`.
10. Kiểm tra trang đăng nhập.
11. Đăng nhập và kiểm tra dữ liệu, phản hồi và SSE.

Phải bật MySQL trước khi chạy JAR. Nếu Spring Boot không kết nối được MySQL trong lúc khởi động, ứng dụng có thể không lên hoàn chỉnh.

`cloudflared agent` tự chạy không có nghĩa PVT tự chạy:

- Tunnel có thể đã kết nối tới Cloudflare.
- MySQL vẫn chưa chạy cho tới khi bật trong Laragon.
- Spring Boot chưa chạy cho tới khi nhấp `run.vbs`.
- Website có thể trả lỗi Cloudflare dù Tunnel vẫn hiển thị `Healthy`.

### 24.9. Trình tự dừng máy chủ

Trình tự nên dùng:

1. Dừng PVT bằng menu `Tắt server` trên System Tray.
2. Xác nhận process Java đã dừng và port 80 được giải phóng.
3. Sau đó mới dừng MySQL trong Laragon nếu cần.
4. Thông thường không cần dừng `cloudflared agent`.

Không thay thế `pvt.jar` khi process Java cũ còn chạy.

### 24.10. Quy trình build và triển khai JAR mới

```text
Máy phát triển
  -> sửa source
  -> kiểm tra Git diff
  -> build JAR
  -> chép JAR sang máy chủ
  -> dừng PVT cũ
  -> thay pvt.jar
  -> chạy run.vbs
  -> kiểm tra bằng domain public
```

Quy trình chi tiết:

1. Kiểm tra Git diff và bảo đảm không đóng gói nhầm thay đổi chưa hoàn tất.
2. Chạy:

```powershell
.\gradlew.bat clean build
```

3. Xác nhận artifact trong `build/libs` vừa được tạo từ source hiện tại.
4. Nếu thay đổi entity hoặc logic ghi dữ liệu, backup MySQL trước khi triển khai.
5. Chép artifact sang máy chủ.
6. Dừng PVT cũ bằng System Tray.
7. Lưu lại JAR cũ để rollback.
8. Đặt JAR mới cạnh `run.vbs`.
9. Đổi tên artifact thành chính xác `pvt.jar`.
10. Giữ MySQL chạy.
11. Nhấp `run.vbs` đúng một lần.
12. Chờ ứng dụng khởi động.
13. Kiểm tra URL public và đăng nhập.
14. Kiểm tra quyền manager/employee.
15. Kiểm tra tải dữ liệu và phản hồi.
16. Kiểm tra SSE giữa hai trình duyệt.
17. Kiểm tra ảnh và video.
18. Nếu lỗi, dừng process mới và khôi phục JAR cũ.

Lưu ý rollback:

- `spring.jpa.hibernate.ddl-auto=update` có thể thay đổi schema khi JAR mới chạy.
- Khôi phục JAR cũ không tự khôi phục schema hoặc dữ liệu.
- Vì chưa có migration, backup database rất quan trọng trước thay đổi entity.
- JAR chứa code và resource; dữ liệu thật nằm trong MySQL và thư mục media.
- Không cần đổi Tunnel khi chỉ thay JAR và vẫn giữ port 80.
- Nếu đổi `server.port`, phải cập nhật service đích trong Cloudflare Tunnel.

### 24.11. Cách hiểu trạng thái Tunnel

Trạng thái `Healthy` chủ yếu cho biết connector đang kết nối với Cloudflare.

Nó không xác nhận:

- Spring Boot đã chạy.
- Port 80 đang lắng nghe.
- MySQL đã chạy.
- Đăng nhập hoạt động.
- Database truy vấn thành công.
- SSE hoạt động.
- Ảnh và video tải được.

Vì vậy Tunnel có thể `Healthy` nhưng website vẫn lỗi 502 hoặc không sử dụng được.

### 24.12. Lỗi vận hành thường gặp

| Hiện tượng | Nội dung cần kiểm tra |
|---|---|
| Tunnel không `Healthy` | `cloudflared agent`, mạng máy chủ hoặc kết nối Cloudflare |
| Tunnel `Healthy` nhưng website lỗi 502 | `pvt.jar` chưa chạy, port 80 không lắng nghe hoặc Spring Boot lỗi |
| Nhấp `run.vbs` nhưng không thấy PVT | MySQL, Java PATH, port 80, tên/vị trí JAR hoặc lỗi ứng dụng |
| Không đăng nhập được bằng localhost | Cookie Secure và việc truy cập HTTP |
| Domain redirect sai HTTP/HTTPS | Forwarded headers hoặc cấu hình proxy |
| Trang mở nhưng dữ liệu lỗi | MySQL, schema hoặc kết nối `localhost:3306/pvt` |
| Realtime không cập nhật | SSE, EventSource, Tunnel hoặc `NotificationService` |
| Ảnh/video không tải | Đường dẫn media, quyền đọc hoặc resource mapping |
| Website mất khi máy chủ tắt | Chỉ có một máy chủ và một replica |
| Chạy JAR mới không được | Process cũ hoặc chương trình khác đang chiếm port 80 |
| Không thấy log lỗi | Ứng dụng chạy nền bằng `javaw` |

### 24.13. Checklist kiểm tra online sau triển khai

- Tunnel `PVT-SERVER` vẫn `Healthy`.
- Chỉ có process PVT mong muốn dùng port 80.
- Domain mở được bằng cửa sổ ẩn danh.
- Cửa sổ ẩn danh hiển thị trang đăng nhập PVT.
- Không redirect sai HTTP/HTTPS.
- Đăng nhập và session hoạt động.
- Nhân viên không thấy menu quản lý ngoài ý muốn.
- URL manager được bảo vệ bằng `ROLE_MANAGER`.
- Trang lịch tải được dữ liệu.
- Thêm, sửa và phản hồi công việc hoạt động.
- Không làm mất dữ liệu hiện có.
- SSE cập nhật giữa hai trình duyệt.
- EventSource reconnect sau mất mạng ngắn.
- Không mất tab hoặc nội dung đang nhập khi có SSE.
- Ảnh và video tải được.
- Kiểm tra desktop và mobile.
- Nếu lỗi, lấy log từ máy chủ thật, không suy luận từ máy phát triển.

### 24.14. Bảo mật và thông tin bí mật

- Không ghi Cloudflare Tunnel token vào repository.
- Không gửi ảnh chứa token hoặc lệnh cài đặt có token.
- Không ghi API key hoặc private key vào tài liệu.
- Không ghi lại password database trong `PROJECT.md`.
- Không commit file cấu hình bí mật mới nếu chưa thống nhất cách quản lý.
- Tunnel không thay thế Spring Security.
- Trang đăng nhập PVT đang public trên Internet.
- Phải giữ phân quyền URL hiện có.
- Kiểm tra manager bằng role, không khóa cứng username.
- Route Tunnel tới `localhost:80` chỉ nên trỏ tới ứng dụng mong muốn.

### 24.15. Điểm chưa được chuẩn hóa

Các thông tin hoặc quy trình sau chưa được ghi nhận đầy đủ:

- Đường dẫn tuyệt đối chứa `run.vbs` và `pvt.jar` trên máy chủ.
- Phiên bản Java/JRE trên máy chủ.
- Phương thức chép JAR sang máy chủ.
- Quy trình backup/restore MySQL định kỳ.
- Cấu hình log file lâu dài.
- Health endpoint cho Spring Boot và database.
- Cảnh báo tự động khi website mất kết nối.
- Cơ chế tự khởi động MySQL sau reboot.
- Cơ chế tự khởi động `pvt.jar` sau reboot.
- Cloudflare WAF, rate limiting hoặc cache rules nếu có.
- Quy trình rollback database khi schema đã thay đổi.
- Cách theo dõi dung lượng thư mục ảnh và video.

Không tự ý tự động hóa hoặc thay đổi các mục này khi chưa được yêu cầu. Khi cách vận hành thay đổi phải cập nhật lại phần triển khai này.
