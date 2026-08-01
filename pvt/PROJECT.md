# PVT — PROJECT KNOWLEDGE BASE

> Tài liệu ngữ cảnh kỹ thuật và nghiệp vụ.
> AI phải đọc toàn bộ file này trước khi phân tích hoặc sửa project.
> Lần đối chiếu trực tiếp với source gần nhất: 08/07/2026.

## 1. Mục đích dự án

PVT là ứng dụng web quản lý công việc nội bộ. Bản đang vận hành hiện tại phục vụ Phòng Vật tư Ống thép Hòa Phát Đà Nẵng tại `https://othpdn.phongvattu.online`.

Hệ thống đã được mở rộng thành ba instance độc lập từ cùng source/JAR:

- Ống thép Hòa Phát: `https://othp.phongvattu.online`.
- Bình Dương - Long An: `https://othpbdla.phongvattu.online`.

Mỗi địa điểm có máy chủ Windows, MySQL, thư mục media và Cloudflare Tunnel riêng. Không dùng ba domain trỏ chung một database nếu nghiệp vụ của ba phòng phải tách biệt.

Tên đơn vị đã được tham số hóa bằng `app.site-name`. JAR không chứa tên đơn vị mặc định; mỗi máy chủ bắt buộc có một file `application.properties` riêng nằm ngay cạnh `pvt.jar`.

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
├── run.vbs
├── Setup Run web PVT.vbs
├── pvt-launcher.ico
├── deployment-configs/
│   ├── da-nang/application.properties
│   ├── othp/application.properties
│   └── bd-la/application.properties
├── integration/google-apps-script/
│   ├── pvt-debt-webapp-addon.gs
│   └── README.md
├── database-null-audit.sql
├── account-birthday-update-template.sql
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

`database-null-audit.sql` là truy vấn chỉ đọc để kiểm tra dữ liệu cũ. `account-birthday-update-template.sql` là mẫu cập nhật ngày sinh và phải sửa theo dữ liệu thật trước khi chạy. Bộ `run.vbs`, setup shortcut và icon là file triển khai ngoài JAR.
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
`app.public-url` là URL mở khi nhấp đúp tray icon; máy chủ online phải đặt domain
HTTPS của site, không dùng `http://localhost` nếu muốn trình duyệt hiện kết nối bảo mật.
Ảnh: E:/IT/images.
Video: E:/IT/videos.
Có cấu hình forwarded headers cho Cloudflare/reverse proxy.
Session cookie đặt secure=true.
Session không hoạt động quá 12 giờ mới hết hạn.
Log ứng dụng: logs/pvt.log.
Access log: logs/access/pvt-access*.log, với working directory là thư mục chứa JAR.
Chưa có profile dev/test/prod riêng.
Username/password database đang được đặt trực tiếp trong file cấu hình. Không ghi lại giá trị password trong tài liệu này.

Spring Boot 2.7 tự đọc file cấu hình ngoài JAR tại thư mục hiện hành. Vì `run.vbs` đặt working directory về thư mục chứa JAR, từng máy chủ đặt `application.properties` ngay cạnh `pvt.jar`. File này bắt buộc khai báo `app.site-name` và có thể ghi đè database, password, đường dẫn media mà không sửa JAR. Không commit bản cấu hình thực tế của máy chủ nếu chứa mật khẩu.
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
Session có timeout không hoạt động là 12 giờ để tránh phải đăng nhập lại nhiều lần trong một ngày làm việc.
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
birthday: chuỗi ngày/tháng `DD/MM`, cho phép null và không lưu năm sinh.
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
monthEventStatus	1	Việc tháng theo luồng cũ; chỉ còn để đọc và quản lý dữ liệu cũ, không tạo mới
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
Field `birthday` là ngày áp dụng của thông điệp đặc biệt, không còn đại diện cho sinh nhật account.
Service lấy tất cả record có cùng ngày/tháng hiện tại, bỏ record thiếu ngày hoặc message rỗng và sắp xếp theo ID.
Sinh nhật thành viên được lấy từ `Account.birthday`, không tạo từng record sinh nhật trong bảng này.
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
PasswordController
GET  /home/account/change-password
POST /home/account/change-password
TrashController
GET  /home/manager/trash
POST /home/manager/trash/move/{id}
POST /home/manager/trash/restore/{id}
POST /home/manager/trash/delete/{id}
POST /home/manager/trash/empty
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
POST /home/manager/task/addMonthEvent

GET  /home/manager/task/showEditMonthEventForm/{id}
POST /home/manager/task/editMonthEvent

GET  /home/manager/task/hide/{id}
GET  /home/manager/task/show/{id}
POST /home/manager/task/toggleHidden/{id}

GET  /home/manager/weeklyTask
GET  /home/manager/weeklyTask/showAddWeekEventForm
POST /home/manager/weeklyTask/addWeekEvent

GET  /home/manager/delete-account
POST /home/manager/delete-account

POST /home/manager/saveTabOrder
Sau các thao tác thay đổi dữ liệu, controller thường gọi:
notificationService.sendRefreshSignal();
ImageController
GET /home/employee/image
GET /home/employee/video
Controller quét subfolder và tạo danh sách media.
ValidationExceptionHandler
Chuyển lỗi bind/validation không được controller xử lý sang trang `validation-error.html` thay vì để rơi vào Whitelabel Error Page.
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
createdByDate của log dùng thời điểm thực hiện thao tác sửa; nội dung log chứa deadline mới.
Việc cập nhật công việc và tạo log phản hồi nằm trong cùng transaction: thành công toàn bộ hoặc rollback toàn bộ.
Nếu lưu lỗi, form phải giữ dữ liệu vừa nhập và hiển thị nguyên nhân thân thiện để người dùng thử lại.
Nút Lưu phải chống submit lặp trong lúc request trước đang xử lý.
Ẩn/hiện
Hiện có hai cơ chế:
GET /hide/{id} và /show/{id}.
POST /toggleHidden/{id}.
Nên thống nhất UX trong tương lai.
Xóa việc
Xóa từ Task, trang việc lặp hoặc trang phản hồi là xóa mềm: đặt `is_deleted=true` và giữ nguyên response.
Chỉ thao tác **Xóa hẳn** hoặc **Làm sạch thư mục** trong `/home/manager/trash` mới xóa response trước rồi xóa MonthEvent.
Các thao tác xóa hẳn nằm trong transaction và khóa bản ghi để tránh xung đột.
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
Khi quản lý đổi tab, URL phải cập nhật `selectedValue` ngay.
Chỉ kết quả của request tab mới nhất được phép cập nhật bảng.
Request cũ phải được hủy để tránh dữ liệu của tab trước ghi đè tab mới.
Nếu session hết hoặc request lỗi, không giữ im lặng nội dung cũ; phải chuyển tới đăng nhập hoặc hiển thị lỗi có nút thử lại.
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
connected -> data "ok"
refresh -> data "update"
ping    -> data "heartbeat"
Heartbeat chạy mỗi 20 giây để Cloudflare không ngắt kết nối im lặng.
Khi subscribe, server gửi `connected` ngay để commit response SSE qua proxy.
Emitter được recycle sau 30 phút; EventSource tự reconnect.
Refresh và heartbeat được gửi tuần tự trên daemon thread riêng, không giữ request POST chờ các trình duyệt khác.
Emitter được xóa khi:
Completion.
Timeout.
Error.
Gửi event thất bại.
SSE chỉ báo “có thay đổi”; client vẫn gọi API để lấy dữ liệu mới.
## 16. Ảnh và video

### 16.1. Logo, banner và icon hệ thống

| Vị trí sử dụng | Nguồn hiện tại | Có cần chép riêng lên máy chủ? |
|---|---|---|
| Navbar và welcome mobile | `src/main/resources/static/img/logo.png` qua `/img/logo.png` | Không; file nằm trong JAR |
| Banner Home trên PC | `src/main/resources/static/img/banner.webp` qua `/img/banner.webp` | Không; file nằm trong JAR |
| Windows System Tray | `src/main/resources/static/img/icon.png` | Không; file nằm trong JAR |
| Shortcut `Run web PVT` | `pvt-launcher.ico` ngoài JAR | Có; chép cạnh launcher và JAR |
| Logo trang đăng nhập | `https://www.hoaphat.com.vn/assets/images/logo.png` | Đang tải trực tiếp từ Internet |

Logo đăng nhập là tài nguyên ngoài hệ thống: nếu website Hòa Phát đổi URL hoặc máy người dùng không tải được domain đó thì logo đăng nhập sẽ mất. Các logo còn lại là local trong JAR. Khi chuẩn hóa đa site nên đổi logo đăng nhập sang `th:src="@{/img/logo.png}"` để không phụ thuộc Internet.

Các dependency giao diện ngoài hệ thống hiện gồm Flatpickr từ jsDelivr, SweetAlert từ unpkg, SortableJS từ jsDelivr và font Be Vietnam Pro từ Google Fonts. Mất kết nối tới các CDN này có thể làm một phần giao diện giảm chức năng dù Spring Boot vẫn chạy.

### 16.2. Thư viện media trên ổ đĩa

Resource mapping:

```text
/img-local/** -> E:/IT/images/
/vid-local/** -> E:/IT/videos/
```

Hai nhóm tài nguyên được đặt cache public 30 ngày.

```text
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
```

Extension ảnh: `jpg`, `jpeg`, `png`, `gif`, `webp`, `bmp`.

Extension video: `mp4`, `webm`, `ogg`, `mov`.

Quy tắc quét thư mục:

- Chỉ đọc các subfolder trực tiếp bên dưới thư mục gốc; file đặt thẳng ở thư mục gốc không hiện.
- Mỗi subfolder có ít nhất một file hợp lệ trở thành một tab album.
- Subfolder và file được sắp xếp tăng dần theo tên.
- Không có chức năng upload trên web; quản trị viên phải chép/xóa file trực tiếp trên máy chủ.
- Tài khoản Windows chạy `javaw` phải có quyền đọc các thư mục và file.
- Nếu máy mới không có ổ `E:`, phải tạo ổ/thư mục tương ứng hoặc ghi đè hai đường dẫn bằng `application.properties` cạnh JAR.
- `/img-local/**` và `/vid-local/**` đang bị Spring Security bỏ qua để cache hiệu quả. Trang album cần đăng nhập, nhưng URL media cụ thể có thể truy cập không cần session nếu biết đường dẫn.
- Vì cache 30 ngày, thay nội dung nhưng giữ nguyên tên file có thể khiến người dùng còn thấy bản cũ; nên đổi tên file hoặc purge cache Cloudflare khi cần cập nhật ngay.

Trang ảnh có tab album, grid, lightbox, next/previous, zoom, drag, scroll wheel và pinch zoom mobile.

Trang video có tab album, video card, lightbox, next/previous và lấy tên file từ URL.

Điểm còn cần sửa:

- Trang ảnh đã có CSS ẩn album không active; trang video chưa có quy tắc `.album-panel`/`.album-panel.active` tương ứng nên cần kiểm tra lại việc ẩn album video không được chọn.
- MIME lightbox video đang cố định là `video/mp4`; file webm/ogg/mov có thể không phát đúng dù controller vẫn liệt kê.
- Cần lazy loading tốt hơn khi số lượng video lớn.
## 17. Danh sách template
Template	Chức năng
login.html	Đăng nhập
home.html	Trang chủ
fragments.html	Header/navbar
private.html	Lịch cần làm
eventResponse.html	Phản hồi
task.html	Quản lý việc thường
privateEventForm.html	Giao việc cá nhân
monthEditEventForm.html	Sửa việc
weeklyTask.html	Danh sách việc lặp
weekEventForm.html	Tạo việc lặp
create-account.html	Tạo account
deleteAccount.html	Xóa account
change-password.html	Đổi mật khẩu
trash.html	Thư mục đã xóa
validation-error.html	Lỗi bind/validation thân thiện
images.html	Album ảnh
video.html	Album video
deny.html	Trang 403

Navbar hiện vẫn hiển thị menu quản lý cho nhân viên. Spring Security chặn URL nhưng định hướng UX là ẩn menu quản lý khỏi nhân viên.
## 18. Tài khoản
Tạo account
Kiểm tra username, password, họ tên, role và birthday.
Password được BCrypt trước khi lưu.
Account và AccountRole được lưu trong cùng transaction.
Birthday là tùy chọn và chuẩn hóa về `DD/MM`.
Database mới phải có sẵn:
ROLE_EMPLOYEE
ROLE_MANAGER
Project chưa có migration hoặc seed.
Xóa account
Không cho quản lý xóa chính mình.
Trong cùng transaction: tìm các event cá nhân của account, xóa response theo từng event, xóa AccountRole, xóa event rồi xóa Account.
Event giao cả phòng có `account=null` nên không bị xóa theo tài khoản.
`UserTabOrder` chưa được dọn trực tiếp khi xóa account; record thứ tự cũ có thể còn trong database nhưng danh sách account hiện tại không render tab đã mất. Lần quản lý lưu lại thứ tự sẽ thay thế danh sách của quản lý đó.
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

Tại lần rà soát 06/07/2026 có 9 class test và 38 method `@Test`:

- `PvtApplicationTests`: tải Spring context.
- `AccountServiceTest`: validation tạo/xóa account và transaction logic.
- `MonthEventServiceTest`: validation batch, sửa, xóa mềm/khôi phục/xóa hẳn.
- `ResponseEventInformationServiceTest`: validation response và event bị xóa.
- `UserTabOrderServiceTest`: body null, account hợp lệ và loại trùng.
- `SpecialMessageServiceTest`: birthday và special day.
- `SercurityScheduleServiceTest`: lọc khối rỗng.
- `NullSafeDisplayTest`: getter hiển thị null-safe.
- `SiteViewModelAdviceTest`: tên đơn vị theo cấu hình và từ chối cấu hình rỗng.

Các test service chủ yếu dùng Mockito nên không cần database thật. Riêng `PvtApplicationTests` dùng `@SpringBootTest`, chưa có profile test riêng và có thể cố kết nối MySQL theo `application.properties`.

Chưa có test chuyên biệt cho controller, repository integration, Spring Security, SSE end-to-end, trình duyệt PC/mobile và media.

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
| Spring Boot | Khởi động thủ công bằng shortcut `Run web PVT` | `localhost:80` |

Laragon chỉ chạy MySQL ở port 3306. Không chạy Apache hoặc Nginx, nhờ đó không tranh chấp port 80 với embedded Tomcat.

Nếu một web server khác hoặc một bản PVT cũ đang giữ port 80, JAR mới sẽ không khởi động được.

### 24.6. Trình khởi động Run web PVT

Bộ khởi động nằm cùng thư mục với `pvt.jar` trên máy chủ:

- `run.vbs`: launcher chạy nền; người vận hành không cần bấm trực tiếp sau khi đã tạo shortcut.
- `pvt-launcher.ico`: icon dùng chung nhận diện với System Tray của PVT.
- `Setup Run web PVT.vbs`: tạo shortcut đúng theo đường dẫn thực tế trên từng máy.
- `Run web PVT.lnk`: shortcut được tạo sau khi chạy file setup và là mục dùng hằng ngày.

Cài đặt một lần:

1. Chép ba file `run.vbs`, `pvt-launcher.ico` và `Setup Run web PVT.vbs` vào cùng thư mục với `pvt.jar`.
2. Nhấp đúp `Setup Run web PVT.vbs`.
3. Script tạo shortcut `Run web PVT` trong thư mục triển khai.
4. Chọn `Yes` nếu muốn sao chép nguyên shortcut vừa tạo ra Desktop. Cơ chế này tương đương `Send to > Desktop`, không tạo lại một shortcut có cấu hình khác.
5. Từ lần sau chỉ nhấp `Run web PVT`.

Không lưu sẵn file `.lnk` trong Git vì shortcut chứa đường dẫn tuyệt đối của máy tạo ra nó. Nếu di chuyển thư mục triển khai, phải chạy lại file setup để cập nhật đường dẫn.

Nội dung cốt lõi của `run.vbs`:

```vbscript
Option Explicit

Dim WshShell, fso, strPath, jarPath, command
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strPath = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = fso.BuildPath(strPath, "pvt.jar")

If Not fso.FileExists(jarPath) Then
    MsgBox "pvt.jar was not found in:" & vbCrLf & strPath, _
        vbCritical, "Run web PVT"
    WScript.Quit 1
End If

' Đảm bảo thư mục logs được tạo cạnh pvt.jar.
WshShell.CurrentDirectory = strPath

' Chạy ứng dụng nền với encoding UTF-8.
command = "javaw -Dfile.encoding=UTF-8 -jar """ & jarPath & """"
WshShell.Run command, 0, False
```

Hành vi:

- Lấy thư mục chứa `run.vbs` làm thư mục triển khai.
- Đặt working directory về thư mục triển khai để log được tạo cạnh `pvt.jar`.
- Kiểm tra JAR có tên chính xác `pvt.jar` tồn tại trong cùng thư mục; nếu thiếu sẽ hiện thông báo.
- Dùng `javaw` nên ứng dụng chạy nền, không mở console.
- Truyền `-Dfile.encoding=UTF-8` ngay khi JVM khởi động.
- Tham số `0` chạy cửa sổ ẩn.
- Tham số `False` làm VBS kết thúc ngay sau khi tạo process Java.
- VBS không giám sát hoặc tự khởi động lại Java khi ứng dụng dừng.
- Nếu không gọi được `javaw`, launcher hiển thị hướng dẫn kiểm tra Java và biến môi trường `PATH`.
- Script không tự chạy cùng Windows; người vận hành phải nhấp shortcut thủ công.

Lưu ý:

- Không nhấp `Run web PVT` nhiều lần khi chưa kiểm tra icon System Tray hoặc process Java.
- Bản chạy thứ hai sẽ cố chiếm lại port 80 và có thể thất bại.
- Vì dùng `javaw`, lỗi khởi động không xuất hiện trong terminal.
- Khi cần chẩn đoán, có thể tạm chạy:

```powershell
java -Dfile.encoding=UTF-8 -jar pvt.jar
```

Lệnh trên cần chạy tại Command Prompt/PowerShell trên máy chủ để xem log trực tiếp.

Không đưa password database, Tunnel token, API token hoặc private key vào launcher hay tài liệu.

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
6. Nhấp shortcut `Run web PVT`.
7. `javaw` chạy `pvt.jar` nền.
8. Chờ icon hoặc thông báo PVT khởi động thành công.
9. Mở `https://othpdn.phongvattu.online`.
10. Kiểm tra trang đăng nhập.
11. Đăng nhập và kiểm tra dữ liệu, phản hồi và SSE.

Phải bật MySQL trước khi chạy JAR. Nếu Spring Boot không kết nối được MySQL trong lúc khởi động, ứng dụng có thể không lên hoàn chỉnh.

`cloudflared agent` tự chạy không có nghĩa PVT tự chạy:

- Tunnel có thể đã kết nối tới Cloudflare.
- MySQL vẫn chưa chạy cho tới khi bật trong Laragon.
- Spring Boot chưa chạy cho tới khi nhấp shortcut `Run web PVT`.
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
  -> chạy Run web PVT
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
8. Đặt JAR mới cạnh bộ launcher PVT.
9. Đổi tên artifact thành chính xác `pvt.jar`.
10. Giữ MySQL chạy.
11. Nhấp shortcut `Run web PVT` đúng một lần.
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
| Nhấp `Run web PVT` nhưng không thấy PVT | MySQL, Java PATH, port 80, tên/vị trí JAR hoặc lỗi ứng dụng |
| Không đăng nhập được bằng localhost | Cookie Secure và việc truy cập HTTP |
| Domain redirect sai HTTP/HTTPS | Forwarded headers hoặc cấu hình proxy |
| Trang mở nhưng dữ liệu lỗi | MySQL, schema hoặc kết nối `localhost:3306/pvt` |
| Realtime không cập nhật | SSE, EventSource, Tunnel hoặc `NotificationService` |
| Ảnh/video không tải | Đường dẫn media, quyền đọc hoặc resource mapping |
| Website mất khi máy chủ tắt | Chỉ có một máy chủ và một replica |
| Chạy JAR mới không được | Process cũ hoặc chương trình khác đang chiếm port 80 |
| Không thấy log lỗi | Ứng dụng chạy nền bằng `javaw` |

Log chẩn đoán được ghi tự động:

- `logs/pvt.log`: log Spring Boot, exception và database; xoay file tối đa 10 MB, giữ 14 file và tối đa 200 MB.
- `logs/access/pvt-access*.log`: request thực sự đã tới embedded Tomcat, gồm thời gian, method, URL, HTTP status và thời gian xử lý.

Khi gặp 502:

1. Ghi lại thời điểm chính xác, URL và Cloudflare Ray ID trên trang lỗi.
2. Tìm request POST cùng thời điểm trong access log.
3. Nếu không có request trong access log, lỗi nằm trước Spring Boot: kiểm tra `cloudflared`, route `localhost:80`, port 80 và log Tunnel.
4. Nếu access log có request, đối chiếu `pvt.log` để tìm exception hoặc thời gian xử lý.
5. Form sửa việc bắt lỗi 502/503/504 ở nền và giữ nguyên dữ liệu để người dùng thử lại; không tự động POST lại vì request cũ có thể đã được xử lý.

### 24.13. Checklist kiểm tra online sau triển khai

- Tunnel `PVT-SERVER` vẫn `Healthy`.
- Chỉ có process PVT mong muốn dùng port 80.
- Domain mở được bằng cửa sổ ẩn danh.
- Cửa sổ ẩn danh hiển thị trang đăng nhập PVT.
- Không redirect sai HTTP/HTTPS.
- Đăng nhập và session hoạt động.
- URL quản lý bị chặn với nhân viên. Giao diện hiện vẫn render menu **Giao việc (chỉ quản lý)** cho nhân viên; việc ẩn menu là backlog UX, không phải dấu hiệu phân quyền URL bị hỏng.
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
- Health endpoint cho Spring Boot và database.
- Cảnh báo tự động khi website mất kết nối.
- Cơ chế tự khởi động MySQL sau reboot.
- Cơ chế tự khởi động `pvt.jar` sau reboot.
- Cloudflare WAF, rate limiting hoặc cache rules nếu có.
- Quy trình rollback database khi schema đã thay đổi.
- Cách theo dõi dung lượng thư mục ảnh và video.

Không tự ý tự động hóa hoặc thay đổi các mục này khi chưa được yêu cầu. Khi cách vận hành thay đổi phải cập nhật lại phần triển khai này.

## 25. Đổi mật khẩu tài khoản

- Mọi tài khoản đã đăng nhập, gồm `ROLE_EMPLOYEE` và `ROLE_MANAGER`, có mục **Đổi mật khẩu** trong menu tên người dùng ở góc trên bên phải, ngay phía trên **Đăng xuất**.
- URL biểu mẫu: `/home/account/change-password`; URL này yêu cầu đăng nhập nhưng không yêu cầu quyền quản lý.
- Người dùng phải nhập mật khẩu hiện tại, mật khẩu mới và nhập lại mật khẩu mới.
- Mật khẩu hiện tại được đối chiếu bằng `BCryptPasswordEncoder.matches`; tuyệt đối không giải mã hoặc ghi mật khẩu vào log.
- Mật khẩu mới phải dài từ 6 đến 72 ký tự, khác mật khẩu hiện tại và hai lần nhập phải trùng nhau.
- Mật khẩu mới được mã hóa BCrypt trước khi lưu vào cột `account.password`.
- Sau khi đổi thành công, phiên hiện tại bị kết thúc và người dùng phải đăng nhập lại bằng mật khẩu mới.
- Khi nhập sai hoặc dữ liệu không hợp lệ, hệ thống chỉ hiển thị lỗi và không thay đổi mật khẩu trong database.

## 26. Thư mục đã xóa và xóa mềm công việc

### 26.1. Mô hình dữ liệu

- `MonthEvent.is_deleted` đánh dấu xóa mềm; mặc định là `false`.
- Hibernate `ddl-auto=update` tạo cột mới khi chạy JAR mới. Dữ liệu hiện có phải được xem là chưa xóa.
- Công việc đã xóa mềm vẫn còn nguyên trong bảng `month_event` và toàn bộ `ResponseEventInformation` liên quan vẫn còn nguyên.
- Các truy vấn trang lịch, trang Task và trang việc lặp lại phải loại những bản ghi có `is_deleted=true`.
- API xem hoặc thêm phản hồi không được phép truy cập công việc đã chuyển vào Thư mục đã xóa.

### 26.2. Giao diện và quyền truy cập

- Menu **Giao việc (chỉ quản lý)** có mục **Thư mục đã xóa** nằm dưới **Giao việc cá nhân lặp lại định kỳ** và trên đường phân cách trước **Tạo tài khoản**.
- URL: `/home/manager/trash`, chỉ `ROLE_MANAGER` được truy cập theo cấu hình `/home/manager/**`.
- Giao diện và phân trang theo trang Task, gồm các cột STT, Nội dung giao việc, Deadline và Hành động.
- **Làm sạch thư mục** nằm cùng vị trí với **Thêm mới công việc** của trang Task.
- Mỗi dòng chỉ có **Xóa hẳn** màu danger và **Khôi phục** màu success.
- **Trở về trang chủ** nằm dưới cùng giống trang Task.
- Tất cả thao tác thay đổi dữ liệu dùng HTTP POST và CSRF; không dùng link GET để xóa.

### 26.3. Quy tắc nghiệp vụ

- **Xóa** từ Task, việc lặp lại hoặc trang phản hồi chỉ đặt `is_deleted=true`; không xóa response.
- **Khôi phục** đặt `is_deleted=false` và `is_hidden=false`, vì vậy công việc trở lại trạng thái Hiển thị.
- Khôi phục giữ nguyên `monthEventStatus`: công việc thường trở lại Task; định nghĩa việc lặp lại trở lại trang việc lặp lại để không làm sai cơ chế định kỳ.
- **Xóa hẳn** chỉ chấp nhận công việc đang ở Thư mục đã xóa. Hệ thống xóa toàn bộ response theo `month_event_id` trước, sau đó mới xóa `MonthEvent`, trong cùng transaction.
- **Làm sạch thư mục** thực hiện cùng quy tắc cho toàn bộ bản ghi đã xóa và trả về số công việc đã xóa.
- Mỗi thao tác xóa, khôi phục hoặc làm sạch thành công phát tín hiệu SSE để các màn hình đang mở cập nhật.
- Không được xóa hẳn một công việc đang hoạt động bằng cách gọi trực tiếp URL.
- Các luồng sửa, thêm response, xóa mềm, khôi phục và xóa hẳn khóa bản ghi `MonthEvent` trong transaction để tránh xung đột khi hai người thao tác đồng thời.

## 27. Validation và tính toàn vẹn dữ liệu

### 27.1. Mốc backup trước thay đổi lớn

- Trạng thái web ổn định trước đợt bổ sung validation ngày 04/07/2026 nằm tại commit `095ca02`.
- Git tag: `backup-2026-07-04-before-validation`.
- Mốc này gồm toàn bộ chức năng đã hoàn thành trước validation: Cloudflare/session, SSE, đổi mật khẩu và Thư mục đã xóa.
- Không xóa hoặc di chuyển tag này. Khi cần rollback code phải kiểm tra database và worktree trước khi chuyển về tag.

### 27.2. Loại công việc được tạo mới

- Giao diện tạo **việc tháng cũ** đã bị loại bỏ; không còn template `monthEventForm.html` hoặc endpoint `showAddMonthEventForm`.
- `/home/manager/task/addMonthEvent` chỉ tạo `monthEventStatus=0`.
- `/home/manager/weeklyTask/addWeekEvent` chỉ tạo `monthEventStatus=2`.
- Dữ liệu cũ có `monthEventStatus=1` vẫn được hiển thị trên trang Task để có thể sửa, xóa mềm hoặc khôi phục; không tự động chuyển đổi hay xóa.
- Giá trị `ALL` chỉ là mã quy ước từ form cho lựa chọn **Cả phòng**. Server chuyển nó thành `is_all=true` và `account=null`; database không cần account tên `ALL`.

### 27.3. Quy tắc validation chính

- Nội dung công việc và phản hồi không được null hoặc chỉ chứa khoảng trắng; project không đặt giới hạn độ dài nghiệp vụ cho hai loại nội dung này.
- Deadline bắt buộc và lỗi bind ngày giờ phải được trả thành thông báo thân thiện.
- Người phụ trách phải tồn tại trong database, trừ lựa chọn `ALL`.
- Việc lặp lại có `extendDay` từ 1 đến 365.
- Batch công việc được kiểm tra toàn bộ trước khi ghi và lưu trong một transaction; một dòng lỗi làm rollback toàn bộ batch.
- Tạo account yêu cầu username, password, họ tên và role hợp lệ. Password dài 6-72 ký tự. Account và role được lưu trong cùng transaction.
- Thêm response phải kiểm tra response, MonthEvent, ID, nội dung và người gửi trước khi dereference hoặc ghi database.
- Thứ tự tab giữ khóa `managerUsername` theo họ tên đầy đủ trong session. Danh sách gửi lên không được null, không chứa tài khoản trống hoặc không tồn tại và được loại trùng trước khi lưu.
- Frontend phải kiểm tra kết quả POST khi lưu response, sửa việc và thứ tự tab; không được báo thành công khi server hoặc Cloudflare trả lỗi.

### 27.4. Dữ liệu cũ có null

- Các getter ngày giờ và JavaScript hiển thị phải null-safe để một record cũ không làm hỏng toàn trang.
- Không tự bịa deadline, ngày giao hoặc ngày phản hồi cho record cũ.
- File `database-null-audit.sql` chỉ chứa truy vấn đọc để kiểm kê dữ liệu null trên database máy chủ trước khi sửa thủ công.
- `lastTimeResponse=null` được coi là chưa có phản hồi; tên hiển thị account null/trắng fallback về account ID.

### 27.5. Tab nhân viên trên mobile

- PC giữ nguyên danh sách tab dạng flex-wrap và quản lý vẫn kéo thả trực tiếp để sắp xếp.
- Mobile có tổng số tab không quá 12, tính cả tab **Tất cả**, vẫn hiển thị tab như cũ.
- Mobile có tổng số tab lớn hơn 12 chuyển sang dropdown. Nút dropdown hiển thị tab đang chọn.
- Dropdown dùng lưới 3 cột, có chiều cao tối đa và cuộn bên trong để danh sách đông người không kéo dài hết trang.
- Thứ tự DOM của lưới là theo hàng: hàng đầu **Tất cả**, order 1, order 2; hàng tiếp theo order 3, order 4, order 5 và tiếp tục tương tự.
- Tab **Tất cả** luôn đứng đầu và không kéo được. Các tab nhân viên giữ thao tác nhấn giữ rồi kéo để sắp xếp; sau khi kéo vẫn POST toàn bộ thứ tự qua `/home/manager/saveTabOrder`.

### 27.6. Sửa lỗi hồi quy sau validation

- Xóa tài khoản phải lấy danh sách `MonthEvent` được giao cho account, xóa response theo từng `monthEventId`, sau đó mới xóa role, MonthEvent và Account trong cùng transaction.
- Không dùng bulk DELETE JPQL đi xuyên quan hệ `ResponseEventInformation.monthEvent.account`, vì có thể phát sinh lỗi truy vấn lúc chạy và trả HTTP 500.
- Form sửa deadline dùng trường `name="account.accountName"` rõ ràng để lựa chọn `ALL` không bị `th:field` ghi đè trạng thái selected.
- `SercurityScheduleService` loại record null hoặc có cả `str1..str4` rỗng/trắng. Khi không còn record có nội dung, khối cảnh báo security không được render.

## 28. Lời chúc sinh nhật và ngày đặc biệt trên Home

### 28.1. Ngày sinh Account

- `Account.birthday` là chuỗi `DD/MM`, không lưu và không xét năm sinh; cột cho phép null để tương thích account cũ.
- Form tạo account có trường ngày sinh không bắt buộc. Backend chuẩn hóa `8/3` thành `08/03`, chấp nhận `29/02` và từ chối ngày không tồn tại như `31/02`.
- Account cũ được cập nhật một lần bằng mẫu `account-birthday-update-template.sql`.
- Mỗi lần mở `/home`, service tìm toàn bộ account có birthday trùng ngày/tháng hiện tại và dùng họ tên đầy đủ; nếu tên null/trắng thì fallback về account ID.
- Một người dùng từ **thành viên**; nhiều người trùng sinh nhật dùng **các thành viên** và hiển thị đầy đủ tất cả tên.

### 28.2. SpecialMessage

- Bảng `special_message` không còn dùng để lưu từng sinh nhật cá nhân. Field `birthday` cũ của entity vẫn được giữ để làm ngày áp dụng cho thông điệp đặc biệt, tránh thay đổi schema và dữ liệu hiện có.
- Trước mắt database giữ các record ngày 08/03 và 20/10 cùng nội dung do quản lý nhập sẵn.
- Service trả toàn bộ SpecialMessage hợp lệ trùng ngày, không chỉ record đầu tiên; record có ngày null hoặc nội dung rỗng bị bỏ qua.
- Nếu một ngày vừa là sinh nhật thành viên vừa là ngày đặc biệt thì Home hiển thị cả hai khối.

### 28.3. Giao diện

- Khối sinh nhật dùng bánh sinh nhật, bóng bay và confetti chuyển động bằng CSS; tên thành viên hiển thị dạng badge.
- Khối ngày đặc biệt dùng hoa và hiệu ứng chuyển động nhẹ. Không phụ thuộc GIF, CDN hoặc ảnh bên ngoài.
- Mobile giảm kích thước trang trí; thiết bị bật `prefers-reduced-motion` sẽ tự tắt animation.
- Khi không có sinh nhật hoặc SpecialMessage phù hợp, toàn bộ vùng celebration không render và banner Home giữ bố cục cũ.

## 29. Triển khai thêm OTHP và Bình Dương - Long An

### 29.1. Kiến trúc ba site

Mục tiêu là ba hệ thống độc lập dùng chung source code, không phải ba domain cùng trỏ vào một server hoặc một database:

| Địa điểm | Domain | Tunnel đề xuất | Origin trên máy chủ | Database |
|---|---|---|---|---|
| Đà Nẵng | `othpdn.phongvattu.online` | `PVT-SERVER` hiện có | `http://localhost:80` | MySQL local `pvt` hiện có |
| Ống thép Hòa Phát | `othp.phongvattu.online` | `PVT-OTHP` | `http://localhost:80` | MySQL local `pvt` trên máy OTHP |
| Bình Dương - Long An | `othpbdla.phongvattu.online` | `PVT-BDLA` | `http://localhost:80` | MySQL local `pvt` trên máy BD-LA |

Mỗi site có riêng:

- Máy chủ Windows và process Java.
- Laragon/MySQL ở `localhost:3306`.
- Database `pvt`; cùng tên không xung đột vì nằm trên hai máy khác nhau.
- Cloudflare Tunnel và token riêng.
- Thư mục ảnh, video, logs và backup riêng.
- Account, công việc, response, thứ tự tab và special message riêng.

Không cấu hình OTHP/BD-LA cùng kết nối database Đà Nẵng trừ khi có quyết định nghiệp vụ muốn dùng chung toàn bộ dữ liệu. Dùng chung database sẽ làm ba phòng thấy/chỉnh cùng account và công việc.

Tên cookie session mặc định có thể giống nhau nhưng cookie là host-only, nên đăng nhập tại ba subdomain là ba session độc lập.

### 29.2. Cấu hình tên đơn vị dùng chung một JAR

`SiteViewModelAdvice` đọc property bắt buộc `app.site-name` từ file ngoài JAR và đưa `siteName` vào toàn bộ Thymeleaf model. Header trong `fragments.html` và welcome mobile trong `home.html` cùng hiển thị `${siteName}`.

Quy tắc:

- Đà Nẵng hiển thị **Phòng vật tư Ống thép Hòa Phát Đà Nẵng**.
- `othp` hiển thị **Phòng vật tư Ống thép Hòa Phát**.
- `othpbdla` hiển thị **Phòng vật tư Ống thép Hòa Phát Bình Dương - Long An**.
- Thiếu property hoặc giá trị rỗng làm ứng dụng không khởi động, nhờ đó không thể âm thầm hiện sai tên miền.
- Tên lấy từ cấu hình của máy chủ, không phụ thuộc request hostname; vì vậy mở bằng `localhost` vẫn hiện đúng đơn vị của máy đó.
- Không build ba JAR riêng.

File `.properties` dùng Unicode escape thay cho chữ tiếng Việt trực tiếp để tránh Windows/Java đọc sai encoding. Ví dụ Đà Nẵng:

```properties
app.site-name=Ph\u00F2ng v\u1EADt t\u01B0 \u1ED0ng th\u00E9p H\u00F2a Ph\u00E1t \u0110\u00E0 N\u1EB5ng
```

Repository có ba file mẫu trong `deployment-configs`. Khi triển khai, chỉ chép file của đúng site ra cạnh `pvt.jar`; không chép cả ba vào cùng thư mục chạy.

Logo và banner hiện không phụ thuộc tên site, có thể dùng chung. Logo trang đăng nhập vẫn là URL Internet như phần 16.1 và nên chuyển sang logo local trước khi triển khai rộng.

### 29.3. Phần mềm và quyền cần có trên mỗi máy mới

- Windows 64-bit ổn định, có tài khoản quản trị để cài service.
- Máy được bật trong thời gian cần phục vụ web và có Internet ổn định.
- Java 8 64-bit, có cả `java.exe` và `javaw.exe` trong `PATH`.
- Laragon có MySQL tương thích với máy Đà Nẵng.
- TablePlus để quản trị database local.
- `cloudflared` bản Windows 64-bit.
- Quyền truy cập Cloudflare account đang quản lý zone `phongvattu.online`.
- JAR và bộ launcher PVT.
- Ổ đĩa đủ dung lượng cho database, log, ảnh, video và backup.

Nên ghi lại phiên bản Java, MySQL và `cloudflared --version` của từng máy. Ưu tiên dùng cùng major version MySQL với Đà Nẵng để giảm khác biệt schema/collation.

### 29.4. Cấu trúc thư mục khuyến nghị trên máy chủ

Ví dụ nếu dùng ổ `D:`:

```text
D:/PVT/
├── pvt.jar
├── application.properties
├── run.vbs
├── Setup Run web PVT.vbs
├── pvt-launcher.ico
├── logs/                         # ứng dụng tự tạo
└── backup/
    ├── jar/
    └── database/

D:/PVT-data/
├── images/
│   ├── Hoat dong phong/
│   └── Su kien/
└── videos/
    ├── Hoat dong phong/
    └── Su kien/
```

Không chép sẵn `Run web PVT.lnk` từ máy phát triển hoặc máy Đà Nẵng vì shortcut chứa đường dẫn tuyệt đối. Chép ba file launcher rồi chạy `Setup Run web PVT.vbs` trên chính máy mới.

Nếu muốn giữ đúng cấu hình mặc định trong JAR thì phải tạo `E:/IT/images` và `E:/IT/videos`. Khuyến nghị dùng cấu trúc rõ ràng như trên và ghi đè đường dẫn bằng file cấu hình ngoài JAR.

### 29.5. Cài Java 8

1. Tải Java 8 x64 từ nguồn tin cậy, ví dụ Eclipse Temurin 8.
2. Cài JRE hoặc JDK; máy chỉ chạy JAR thì JRE đủ, nhưng JDK tiện cho chẩn đoán.
3. Trong trình cài đặt, bật tùy chọn thêm Java vào `PATH` nếu có.
4. Mở PowerShell/CMD mới và kiểm tra:

```powershell
java -version
where.exe java
where.exe javaw
```

5. `java -version` phải chạy được và `javaw.exe` phải có đường dẫn. Nếu chỉ `java` chạy nhưng `javaw` không có, shortcut nền sẽ không khởi động được.

Project compile target Java 8. Không tự nâng source compatibility hoặc Spring Boot trong lúc dựng hai máy mới.

### 29.6. Cài Laragon và MySQL

Laragon là môi trường quản lý dịch vụ; TablePlus không phải database server.

1. Tải và cài Laragon từ trang chính thức.
2. Chọn thư mục cài đặt cố định, tránh đặt trong thư mục tạm hoặc Downloads.
3. Bảo đảm Laragon có MySQL. Nếu gói cài chỉ có MariaDB hoặc thiếu database, dùng chức năng thêm version của Laragon và ưu tiên cùng loại/version với máy Đà Nẵng.
4. Mở Laragon và chỉ bật MySQL.
5. Không bật Apache/Nginx vì chúng thường chiếm port 80 mà Spring Boot cần.
6. Không cài thêm một MySQL Windows Service khác chạy đồng thời trên port 3306. Nếu máy đã có MySQL trước đó, chọn một bản duy nhất và tắt/gỡ bản không dùng.
7. Kiểm tra port:

```powershell
Test-NetConnection localhost -Port 3306
Get-NetTCPConnection -LocalPort 3306 -State Listen
```

8. Xác định executable/process đang giữ port 3306 để chắc chắn đó là MySQL của Laragon.
9. Ghi lại data directory của MySQL để phục vụ backup, nhưng không sao chép nóng thư mục data khi MySQL đang chạy.

Trình tự vận hành hiện tại giữ giống Đà Nẵng: sau reboot mở Laragon, bật MySQL, chờ sẵn sàng rồi mới chạy PVT.

### 29.7. Cài TablePlus và kết nối database local

1. Cài TablePlus for Windows.
2. Chọn **Create a new connection**.
3. Chọn driver **MySQL**.
4. Điền kết nối khởi tạo:

| Trường | Giá trị |
|---|---|
| Name | `PVT HN Local` hoặc `PVT BD Local` |
| Host | `localhost` |
| Port | `3306` |
| User | tài khoản quản trị MySQL ban đầu |
| Password | password thực tế trên máy đó |
| Database | có thể để trống trước khi tạo `pvt` |
| SSL | không cần cho kết nối loopback local |

5. Chọn **Test**, sau đó **Connect**.
6. Không mở port 3306 ra Internet chỉ để dùng TablePlus. Cài TablePlus ngay trên máy chủ hoặc truy cập máy chủ qua kênh quản trị an toàn hiện có.

TablePlus lưu thông tin kết nối trên máy người dùng và chỉ là GUI. Việc đóng TablePlus không làm MySQL hoặc website dừng.

### 29.8. Tạo database và tài khoản ứng dụng

Khuyến nghị không dùng `root` làm tài khoản chạy ứng dụng mới. Trong TablePlus, mở SQL Query và chạy với password mạnh riêng cho từng máy:

```sql
CREATE DATABASE IF NOT EXISTS pvt
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'pvt_app'@'localhost'
  IDENTIFIED BY 'THAY_PASSWORD_RIENG_CUA_MAY_CHU';

GRANT ALL PRIVILEGES ON pvt.* TO 'pvt_app'@'localhost';
FLUSH PRIVILEGES;
```

Ứng dụng hiện dùng `ddl-auto=update`, nên user `pvt_app` phải có quyền tạo/sửa bảng trong database `pvt`. Không ghi password thật vào `PROJECT.md`, Git, ảnh chụp Cloudflare hoặc tin nhắn công khai.

Sau đó tạo một connection TablePlus thứ hai bằng `pvt_app`, chọn database `pvt` và Test. Connection root chỉ dùng khi thực sự cần quản trị.

Nếu MySQL báo chính sách authentication không tương thích, trước tiên đối chiếu version MySQL/Connector và log ứng dụng; không hạ authentication hoặc bật truy cập từ mọi host một cách tùy tiện.

### 29.9. Tạo cấu hình riêng nằm cạnh JAR

Mỗi máy chỉ có một file `D:/PVT/application.properties`, nằm cùng cấp với `pvt.jar`. Ví dụ BD-LA:

```properties
app.site-name=Ph\u00F2ng v\u1EADt t\u01B0 \u1ED0ng th\u00E9p H\u00F2a Ph\u00E1t B\u00ECnh D\u01B0\u01A1ng - Long An

spring.datasource.url=jdbc:mysql://localhost:3306/pvt?useSSL=false&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true
spring.datasource.username=pvt_app
spring.datasource.password=THAY_PASSWORD_RIENG_CUA_MAY_CHU

image.folder.path=D:/PVT-data/images
video.folder.path=D:/PVT-data/videos

server.port=80
```

Không cần chép lại toàn bộ `application.properties` bên trong JAR; chỉ ghi các khóa cần override. Các cấu hình session, log, forwarded header và Hibernate còn lại tiếp tục lấy từ JAR.

`run.vbs` đặt working directory về `D:/PVT`, nên Spring Boot tự tìm `D:/PVT/application.properties`. Nếu chạy JAR thủ công từ thư mục khác, file ngoài có thể không được tìm thấy; phải `cd` về thư mục JAR trước.

Source hiện đã hỗ trợ `app.site-name`; không cần `app.site-code` cho việc hiển thị.

### 29.10. Khởi tạo schema và dữ liệu nền

Có hai phương án, phải chọn rõ trước khi làm.

#### Phương án A — Database trắng cho từng phòng, khuyến nghị

1. Bật MySQL.
2. Chạy JAR lần đầu để Hibernate tạo/cập nhật bảng.
3. Kiểm tra `logs/pvt.log`, bảo đảm không có lỗi datasource/schema.
4. Dừng PVT bằng System Tray trước khi chỉnh dữ liệu nền lớn.
5. Trong TablePlus kiểm tra các bảng đã xuất hiện.
6. Tạo hai role chính xác: `ROLE_EMPLOYEE` và `ROLE_MANAGER`.
7. Tạo account quản lý bootstrap có password BCrypt và liên kết `AccountRole` với `ROLE_MANAGER`.
8. Khởi động PVT, đăng nhập bằng bootstrap manager, tạo manager thật của phòng mới qua giao diện.
9. Đăng xuất, đăng nhập manager mới, rồi mới xóa bootstrap manager nếu không cần giữ.
10. Chép hai record `SpecialMessage` ngày 08/03 và 20/10 từ database Đà Nẵng nếu muốn giữ nội dung hiện tại.
11. `SercuritySchedule` có thể để trống; khối giao diện sẽ không render nếu không có nội dung.

Không nhập password thô trực tiếp vào cột `account.password`; cột này phải chứa BCrypt hash. Cách ít lỗi nhất là export một account manager bootstrap đã biết mật khẩu cùng record `AccountRole` từ Đà Nẵng, import vào database mới, sau đó dùng giao diện tạo manager mới và loại bootstrap.

Khi export bootstrap, phải chép đúng các quan hệ cần thiết:

- Hai row trong `role` hoặc tạo role trước.
- Một row manager trong `account`.
- Row tương ứng trong `account_role`.

Không cần và không nên chép `month_event`, `response_event_information` hoặc `user_tab_order` nếu muốn site mới bắt đầu sạch.

#### Phương án B — Clone toàn bộ database Đà Nẵng

Chỉ dùng nếu thực sự muốn hai site bắt đầu với toàn bộ account, task, response, thứ tự tab, special message và dữ liệu khác của Đà Nẵng.

1. Dừng hoặc hạn chế ghi dữ liệu Đà Nẵng trong lúc tạo dump nhất quán.
2. Export toàn bộ database `pvt` thành SQL bằng TablePlus hoặc `mysqldump`.
3. Tạo database `pvt` trên máy mới.
4. Import dump trước khi mở site cho người dùng.
5. Chạy JAR để Hibernate bổ sung schema còn thiếu nếu cần.
6. Kiểm tra account/role, số lượng event/response và dữ liệu null.

Sau khi clone, ba database vẫn độc lập; thay đổi sau đó không tự đồng bộ. Không copy trực tiếp thư mục data của MySQL giữa các version hoặc khi MySQL đang chạy.

### 29.11. Tạo thư mục ảnh và video

1. Tạo hai thư mục gốc đúng với `application.properties` cạnh JAR.
2. Tạo ít nhất một subfolder album bên trong mỗi thư mục.
3. Chép file media vào subfolder, không đặt thẳng ở root.

```text
D:/PVT-data/images/Su kien khai truong/anh-01.jpg
D:/PVT-data/images/Hoat dong phong/anh-02.webp
D:/PVT-data/videos/Hoat dong phong/video-01.mp4
```

4. Dùng tên file không chứa ký tự điều khiển; tên tiếng Việt và khoảng trắng có thể dùng nhưng tên ngắn, rõ ràng sẽ ít lỗi URL hơn.
5. Ưu tiên ảnh `jpg/webp` và video `mp4` để tương thích trình duyệt hiện tại tốt nhất.
6. Cấp quyền Read/Read & Execute cho tài khoản Windows vận hành PVT.
7. Mở trang ảnh/video và kiểm tra album, thumbnail, lightbox.

Media không nằm trong MySQL và không nằm trong JAR. Backup database không bao gồm ảnh/video; phải backup hai thư mục này riêng.

### 29.12. Chép JAR và tạo shortcut

Chép vào thư mục triển khai:

- `pvt.jar` vừa build.
- `run.vbs`.
- `Setup Run web PVT.vbs`.
- `pvt-launcher.ico`.
- `application.properties` riêng của máy, đặt cạnh JAR.

Sau đó:

1. Bật MySQL.
2. Nhấp `Setup Run web PVT.vbs`.
3. Chọn tạo shortcut Desktop nếu cần.
4. Từ lần sau chỉ chạy `Run web PVT`.
5. Không chép `.lnk` từ máy khác.
6. Nếu di chuyển thư mục JAR, chạy lại setup shortcut.

Khi chẩn đoán lần đầu, nên chạy foreground để thấy lỗi:

```powershell
Set-Location D:/PVT
java -Dfile.encoding=UTF-8 -jar pvt.jar
```

Chỉ chuyển sang `javaw`/shortcut sau khi app kết nối database và nghe port 80 thành công.

### 29.13. Tạo Cloudflare Tunnel cho từng máy

Tài liệu Cloudflare hiện hành dùng luồng dashboard sau:

1. Đăng nhập `https://dash.cloudflare.com/` bằng account đang quản lý domain.
2. Vào **Networking → Tunnels**.
3. Chọn **Create Tunnel**.
4. Đặt tên riêng, ví dụ `PVT-OTHP` hoặc `PVT-BDLA`.
5. Chọn môi trường Windows và đúng kiến trúc 64-bit.
6. Dashboard cung cấp lệnh Install and Run có Tunnel token.
7. Trên đúng máy chủ của site, mở Command Prompt/PowerShell bằng **Run as administrator**.
8. Cài `cloudflared` bản Windows x64/AMD64 nếu máy chưa có.
9. Chạy đúng lệnh dashboard cung cấp, dạng:

```powershell
cloudflared.exe service install <TUNNEL_TOKEN>
```

10. Không lưu/chụp/gửi token. Nếu token lộ, rotate token trong dashboard và cài lại connector.
11. Chờ connector kết nối và Tunnel chuyển `Healthy`.
12. Mở Tunnel → **Routes** → **Add route** → **Published application**.
13. Site OTHP: chọn subdomain `othp`, domain `phongvattu.online`.
14. Site Bình Dương - Long An: chọn subdomain `othpbdla`, domain `phongvattu.online`.
15. Service URL của cả hai: `http://localhost:80`.
16. Chọn **Add route**.

Khi thêm Published application bằng dashboard, Cloudflare thường tự tạo DNS CNAME proxied từ hostname tới `<TUNNEL-UUID>.cfargotunnel.com`. Sau khi lưu phải vào DNS của `phongvattu.online` để xác nhận record đã có. Không cần tự tạo thêm A record tới IP máy chủ.

Cách kiểm tra DNS trên dashboard:

1. Quay về trang chính Cloudflare, chọn domain `phongvattu.online`.
2. Vào **DNS → Records**.
3. Dùng ô Search DNS records và tìm `othp` hoặc `othpbdla`.
4. Record đúng có dạng:

| Site | Type | Name | Target/Content | Proxy status | TTL |
|---|---|---|---|---|---|
| OTHP | `CNAME` | `othp` | `<UUID-TUNNEL-OTHP>.cfargotunnel.com` | `Proxied`/mây cam | `Auto` |
| BD-LA | `CNAME` | `othpbdla` | `<UUID-TUNNEL-BDLA>.cfargotunnel.com` | `Proxied`/mây cam | `Auto` |

Nếu record đã đúng thì không bấm Add record và không sửa gì thêm. Nếu chưa có record dù Published application route đã lưu thành công, chọn **Add record**, nhập Type `CNAME`, Name là `othp` hoặc `othpbdla`, Target là `<Tunnel UUID>.cfargotunnel.com`, bật **Proxied**, để TTL `Auto`, rồi Save. Tunnel UUID xem tại **Networking → Tunnels → chọn Tunnel → Overview**.

Không tạo A/AAAA cho cùng hostname vì Tunnel không dùng public IP máy chủ; A/AAAA còn có thể xung đột với CNAME.

Nếu báo hostname đã tồn tại, kiểm tra và xử lý A/AAAA/CNAME cũ của `othp` hoặc `othpbdla`; không tạo hai record xung đột.

Mỗi máy mới chỉ cần một Tunnel/connector cho PVT. Một Windows host thông thường chỉ có một service `cloudflared` mặc định; nếu báo service đã tồn tại thì kiểm tra service cũ trước, không chạy nhiều lệnh install chồng lên nhau.

Trong `services.msc`, kiểm tra service Cloudflare:

- Status: `Running`.
- Startup Type: `Automatic`.
- Tên hiển thị có thể là `cloudflared` hoặc `cloudflared agent` tùy cách cài/version; không khóa cứng tên khi kiểm tra máy mới.

Tunnel là kết nối outbound. Không cần port-forward từ router và không cần mở inbound port 80 ra Internet. Nếu firewall outbound chặt, cho phép DNS và port `7844` cả TCP/UDP tới Cloudflare Tunnel endpoints; QUIC dùng UDP và HTTP/2 fallback dùng TCP.

Không bật Cloudflare Access trước PVT nếu muốn giữ hành vi giống Đà Nẵng. Nếu sau này bật Access, phải cập nhật tài liệu vì người dùng sẽ gặp thêm một lớp đăng nhập.

### 29.14. Thứ tự khởi động chuẩn trên mỗi site

1. Windows khởi động.
2. Kiểm tra `cloudflared` đã tự chạy.
3. Mở Laragon.
4. Chỉ bật MySQL.
5. Kiểm tra port 3306 sẵn sàng.
6. Nhấp `Run web PVT` một lần.
7. Chờ icon System Tray và log khởi động.
8. Kiểm tra local port 80:

```powershell
Test-NetConnection localhost -Port 80
```

9. Mở domain public bằng cửa sổ ẩn danh.
10. Đăng nhập và kiểm tra chức năng.

Tunnel `Healthy` trước bước 6 chỉ có nghĩa connector đã nối Cloudflare; website vẫn có thể 502 nếu MySQL hoặc JAR chưa chạy.

### 29.15. Checklist nghiệm thu site mới

- Domain đúng địa điểm và HTTPS hợp lệ.
- Header/welcome hiển thị đúng `app.site-name` trên site OTHP và BD-LA.
- Cửa sổ ẩn danh vào domain thấy login PVT.
- Logo login, navbar, banner và icon hiển thị.
- Manager đăng nhập được; employee bị chặn tại `/home/manager/**`.
- Hai role có đúng tên.
- Tạo, sửa, ẩn/hiện, xóa mềm, khôi phục và xóa hẳn công việc hoạt động.
- Giao cá nhân và **Cả phòng** hoạt động; database không cần account `ALL`.
- Phản hồi và đổi deadline hoạt động.
- SSE cập nhật giữa hai browser và tự reconnect.
- Session không bắt đăng nhập lại bất thường trong ngày làm việc.
- Sinh nhật, 08/03 và 20/10 hiển thị đúng khi có dữ liệu.
- Ảnh/video đọc đúng thư mục riêng của site.
- `logs/pvt.log` và access log được tạo cạnh JAR.
- Tunnel đúng hostname và đúng máy; không trỏ nhầm site.
- Reboot thử một lần và thực hiện lại đúng trình tự khởi động.
- Tạo backup database ban đầu sau khi cấu hình xong.

### 29.16. Backup và cập nhật ba site

Mỗi site phải backup riêng ba nhóm:

1. Database MySQL `pvt` dưới dạng SQL dump.
2. Hai thư mục media ảnh/video.
3. JAR đang chạy và `application.properties` cạnh JAR của site.

Quy trình backup bằng TablePlus:

1. Kết nối đúng site và xác nhận đang mở database `pvt`, tránh export nhầm site.
2. Dùng chức năng Export/Backup, chọn định dạng SQL và gồm cả structure lẫn data.
3. Lưu file theo mẫu `pvt-HN-YYYYMMDD-HHmm.sql` hoặc `pvt-BD-YYYYMMDD-HHmm.sql`.
4. Kiểm tra file không rỗng và có các bảng quan trọng như `account`, `account_role`, `month_event`, `response_event_information`.
5. Định kỳ thử import file vào một database kiểm tra khác; file chưa từng restore thử chưa thể xem là backup chắc chắn.

Khi restore, ưu tiên database rỗng, đọc log import, kiểm tra số row và quan hệ trước khi cho người dùng truy cập. Không import dump của HN vào BD hoặc ngược lại nếu không chủ ý thay toàn bộ dữ liệu site đích.

Trước khi triển khai JAR mới:

- Xác định site nào sẽ cập nhật; không mặc định cả ba đã cùng version.
- Backup database nếu entity/schema thay đổi.
- Lưu JAR cũ với ngày giờ.
- Dừng app bằng System Tray rồi mới thay JAR.
- Không thay `application.properties` của site bằng file từ site khác.
- Kiểm tra lần lượt từng domain sau cập nhật.

Nên có bảng theo dõi version triển khai:

| Site | SHA-256/phiên bản JAR | Ngày triển khai | Người triển khai | Backup DB |
|---|---|---|---|---|
| DN |  |  |  |  |
| OTHP |  |  |  |  |
| BDLA |  |  |  |  |

### 29.17. Lỗi thường gặp khi dựng site mới

| Hiện tượng | Nguyên nhân ưu tiên kiểm tra |
|---|---|
| Tunnel `Healthy`, domain 502 | JAR chưa chạy, port 80 chưa listen hoặc app lỗi DB |
| Tunnel không `Healthy` | Token/service/firewall outbound/DNS/Internet |
| Cloudflare lỗi hostname/DNS | Record cũ xung đột hoặc route gắn sai Tunnel |
| PVT không lên, port 80 bị chiếm | Apache/Nginx Laragon, IIS hoặc process Java cũ |
| PVT không kết nối DB | MySQL chưa bật, sai password ngoài JAR, sai quyền hoặc port 3306 |
| Schema có nhưng không đăng nhập được | Chưa seed role/account bootstrap/AccountRole |
| PVT không khởi động và báo thiếu `app.site-name` | Thiếu/sai `application.properties`, file không nằm cạnh JAR hoặc chạy JAR từ sai working directory |
| Logo login mất | URL logo ngoài `hoaphat.com.vn` không tải được |
| Navbar/banner mất | JAR/resource sai hoặc cache; các file này không lấy từ thư mục media |
| Album trống | Sai path, không có subfolder, extension không hỗ trợ hoặc thiếu quyền đọc |
| Thay ảnh nhưng vẫn thấy ảnh cũ | Cache trình duyệt/Cloudflare 30 ngày; đổi tên hoặc purge cache |
| Shortcut Desktop chạy sai | Shortcut cũ trỏ đường dẫn máy khác; chạy lại setup trên chính máy |
| Không thấy lỗi vì chạy nền | Chạy `java -jar` foreground và đọc `logs/pvt.log` |

### 29.18. Tài liệu chính thức tham khảo

- Cloudflare Tunnel setup: `https://developers.cloudflare.com/tunnel/setup/`.
- Cloudflare Tunnel routing/DNS: `https://developers.cloudflare.com/tunnel/routing/`.
- Cloudflare firewall/port 7844: `https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/tunnel-with-firewall/`.
- Cloudflared Windows download: `https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/`.
- Spring Boot external configuration: `https://docs.spring.io/spring-boot/docs/2.7.x/reference/htmlsingle/#features.external-config`.
- Laragon installation: `https://laragon.org/docs/installation/`.
- TablePlus Windows: `https://tableplus.com/windows`.
- TablePlus connection guide: `https://docs.tableplus.com/gui-tools/manage-connections`.
- Eclipse Temurin downloads: `https://adoptium.net/temurin/releases/`.

Cloudflare dashboard có thể đổi nhãn menu theo thời gian. Tại lần xác minh 06/07/2026, luồng chính là **Networking → Tunnels → Create Tunnel**, sau đó **Routes → Add route → Published application**.

## 30. Phiên bản v1.0 và nâng cấp v2.0

### 30.1. Mốc v1.0 để rollback

Trước khi phát triển ưu tiên công việc ngày 07/07/2026, trạng thái source/JAR đang chạy ổn đã được đóng băng thành **v1.0**:

- Source ZIP: `build/backups/pvt-v1.0-source.zip`.
- JAR: `build/libs/pvt-v1.0.jar`.
- SHA-256 source ZIP: `2388DEFCA4F49529FFF31CB33352AC95746595282E2D5DD3D7EDFBCC6FD150D8`.
- SHA-256 JAR v1.0: `AF1DA5B2BFCDF809F9ED300CF2A74C6766AFE88C7F358BA62EC6AF49173ED721`.

Hai file backup nằm dưới `build/`, không được Git quản lý. Không chạy build clean trước khi đã chép backup sang nơi lưu trữ khác nếu còn cần rollback tại máy phát triển.

Khi rollback production từ v2.0 về v1.0:

1. Dừng PVT.
2. Backup database hiện tại.
3. Thay `pvt.jar` bằng `pvt-v1.0.jar` và đổi tên lại thành `pvt.jar`.
4. Không xóa hai bảng ưu tiên của v2.0; v1.0 không sử dụng chúng nên có thể để nguyên.
5. Giữ đúng `application.properties` riêng của site.

### 30.2. Phạm vi v2.0

v2.0 bổ sung hai nhóm nghiệp vụ:

1. Nhân viên tự giao việc cho chính mình.
2. Chọn và quản lý ưu tiên công việc theo từng ngày.

Artifact đã build ngày 07/07/2026:

- File triển khai giữ tên tương thích launcher: `build/libs/pvt.jar`.
- Bản lưu có version: `build/libs/pvt-v2.0.jar`.
- SHA-256: `5F34B1A742854C0CB11DF0336BA236D9852FED7B7D2B9D332C7BE8CF1CDEA17F`.
- Kết quả kiểm thử: 50 test, 0 failure, 0 error, 0 skipped.

Các chức năng v1.0 như giao việc quản lý, việc lặp lại, phản hồi, đổi deadline, xóa mềm, album, sinh nhật, SSE và Cloudflare không thay đổi nguyên tắc hoạt động.

### 30.3. Giao việc bản thân

Menu dành riêng cho tài khoản chỉ có `ROLE_EMPLOYEE`:

```text
Giao việc
├── Giao việc bản thân
└── Chọn ưu tiên công việc
```

Route:

- `GET /home/employee/self-task`: mở form.
- `POST /home/employee/self-task`: tạo công việc.

Backend luôn lấy `Principal.getName()` và tự gán `MonthEvent.account` bằng account đang đăng nhập. Form không nhận người phụ trách, không nhận `ALL`, không nhận `hidden/deleted` và không tin bất kỳ account name nào từ trình duyệt.

Công việc tự giao có:

- `month_event_status = 0`.
- `is_all = false`.
- `is_hidden = false`.
- `is_deleted = false`.
- `registered_day` là thời điểm lưu.
- Nội dung bắt buộc, không giới hạn độ dài ngoài kiểu `MEDIUMTEXT` hiện tại.
- Deadline bắt buộc.

Nhân viên không thể sửa deadline, xóa hoặc ẩn/hiện vì toàn bộ route quản trị các thao tác này vẫn nằm dưới `/home/manager/**`. Quản lý không thấy menu tự giao việc và controller cũng từ chối tài khoản có `ROLE_MANAGER` truy cập trực tiếp route employee này.

### 30.4. Mô hình dữ liệu ưu tiên

Hibernate `ddl-auto=update` tự tạo hai bảng mới khi chạy JAR v2.0:

#### `daily_priority_selection`

Đây là bản ghi xác nhận một account đã gửi lựa chọn trong một ngày, kể cả khi không ưu tiên công việc nào.

- `selection_id`: khóa chính.
- `account_name`: account được chọn ưu tiên.
- `priority_date`: ngày nghiệp vụ.
- `submitted_at`: thời điểm lưu gần nhất.
- `submitted_by`: người thực hiện; có thể là chính employee hoặc manager.
- Unique `(account_name, priority_date)` để chặn hai lần chọn trong cùng ngày.

#### `task_priority`

Chỉ lưu các công việc thực sự được chọn mức 1–7.

- `task_priority_id`: khóa chính.
- `selection_id`: liên kết ngày chọn.
- `month_event_id`: công việc.
- `priority_value`: số từ 1 đến 7.
- Unique `(selection_id, month_event_id)`.

Không lưu priority trực tiếp vào `month_event`, vì priority thay đổi theo account và theo ngày. Việc Cả phòng là một `MonthEvent` chung, không có account `ALL` trong database và tuyệt đối không tạo `task_priority` cho loại việc này.

### 30.5. Quy tắc ưu tiên của nhân viên

Route:

- `GET /home/employee/priorities`.
- `POST /home/employee/priorities`.

Quy tắc backend:

- Chỉ áp dụng cho chính `Principal.getName()`; account từ form bị bỏ qua.
- Mỗi ngày chỉ POST thành công một lần.
- Tối đa 7 **công việc** có priority khác null.
- Giá trị hợp lệ là số nguyên 1–7.
- Cho phép nhiều công việc trùng cùng một mức, ví dụ hai việc cùng ưu tiên 1.
- Giá trị trống hoặc `KHÔNG ƯU TIÊN` đều không tạo row `task_priority`.
- Việc Cả phòng vẫn hiện trong bảng nhưng select bị disabled; backend cũng từ chối nếu request bị sửa để gửi priority cho việc này.
- Công việc phải còn tồn tại, chưa xóa và đang hiển thị. Trang chọn ưu tiên chỉ lấy deadline quá hạn trong cửa sổ 730 ngày hiện có đến hết ngày mai; kỹ thuật dùng cận trên độc quyền `00:00 ngày kia`. Ví dụ ngày 07/07 sẽ không lấy việc từ 00:00 ngày 09/07 trở đi.
- Sau khi đã gửi, employee chỉ được xem lại; không được sửa trong cùng ngày.

### 30.6. Quản lý ưu tiên

Manager có thêm đúng một mục trong menu **Giao việc (chỉ quản lý)**:

```text
Quản lý ưu tiên công việc
```

Route:

- `GET /home/manager/priorities?accountName=...`.
- `POST /home/manager/priorities`.

Manager chọn một account bất kỳ, gồm cả bản thân. Nếu account chưa có `daily_priority_selection` hôm nay, trang ở chế độ chọn mới. Nếu đã có, bảng thêm:

- **Ưu tiên đã chọn**: chỉ hiện số hoặc dấu `-`.
- **Sửa ưu tiên**: select `KHÔNG ƯU TIÊN`, `ƯU TIÊN 1` đến `ƯU TIÊN 7`.

Manager được sửa nhiều lần trong ngày nhưng vẫn chịu validation tối đa 7 công việc, mức 1–7 và không ưu tiên việc Cả phòng. Khi manager lưu thay employee, employee được xem là đã chọn trong ngày và không thể POST lại.

### 30.7. Reset sau 0 giờ và hiển thị private

Ngày nghiệp vụ lấy theo `Clock` cố định zone `Asia/Ho_Chi_Minh`, không phụ thuộc timezone cấu hình sai trên Windows. Khi ngày đổi:

- Service chỉ truy vấn `priority_date` của ngày mới.
- Dữ liệu ngày cũ vẫn giữ để toàn vẹn lịch sử nhưng không hiển thị.
- Employee lại được chọn đúng một lần cho ngày mới.
- Trang private và trang ưu tiên đang mở tự reload lúc khoảng `00:00:02` để không giữ giao diện ngày cũ.

Trang `/home/employee/private` hiển thị priority ngay trước nội dung:

```text
(1) Báo cáo mục ABC
```

Số trong ngoặc có màu đỏ, nội dung và thứ tự danh sách deadline giữ nguyên. Cả lần render Thymeleaf đầu tiên lẫn API `/home/employee/filterRestful` đều phải gắn `dailyPriority`; SSE refresh cũng nhận kết quả mới.

Employee gọi `filterRestful` luôn bị backend ép về account đăng nhập; chỉ manager mới được truyền `selectedValue` của account khác.

### 30.8. Quan hệ với sửa/xóa dữ liệu

- Khi đổi người phụ trách hoặc chuyển một việc cá nhân thành Cả phòng, priority hiện có của event bị xóa.
- Xóa mềm giữ dữ liệu priority nhưng event không còn xuất hiện trong danh sách đang chọn.
- Xóa hẳn một event phải xóa `task_priority` trước response và `month_event` để không vướng foreign key.
- Làm sạch Thư mục đã xóa áp dụng cùng thứ tự trên cho từng event.
- Xóa account phải xóa priority theo event và theo account trước khi xóa `month_event`/`account`.
- Các row `daily_priority_selection` cũ không gây ảnh hưởng v1.0 nếu rollback.

### 30.9. Các class/template v2.0 quan trọng

```text
config/TimeConfig.java
controller/SelfTaskController.java
controller/PriorityController.java
model/dto/SelfTaskForm.java
model/dto/DailyPriorityForm.java
model/dto/PriorityTaskRow.java
model/priority/DailyPrioritySelection.java
model/priority/TaskPriority.java
repository/priority/IDailyPrioritySelectionRepository.java
repository/priority/ITaskPriorityRepository.java
service/priority/WorkPriorityService.java
templates/selfTaskForm.html
templates/priority.html
```

`MonthEvent.dailyPriority` là field `@Transient`, chỉ phục vụ render/JSON, không tạo cột trong `month_event`.

### 30.10. Checklist triển khai v2.0

1. Backup database từng site trước khi thay JAR vì v2.0 tạo thêm bảng.
2. Lưu JAR v1.0 đang chạy.
3. Chép JAR v2.0 thành `pvt.jar`; không ghi đè file cấu hình ngoài.
4. Bật MySQL và chạy JAR foreground lần đầu.
5. Trong log xác nhận không có lỗi tạo FK/table.
6. Trong TablePlus kiểm tra `daily_priority_selection` và `task_priority`.
7. Đăng nhập employee: tự giao việc, kiểm tra account được ép đúng chính mình.
8. Employee chọn tối đa 7 việc, thử trùng mức và xác nhận lần POST thứ hai bị chặn.
9. Xác nhận việc Cả phòng bị disabled.
10. Đăng nhập manager: chọn employee chưa có ưu tiên, sau đó mở lại để sửa.
11. Kiểm tra `(n)` màu đỏ ở private trên PC/mobile và sau chuyển tab manager.
12. Kiểm tra xóa hẳn event/account không phát sinh lỗi foreign key.
13. Kiểm tra domain của cả ba site và access log sau cập nhật.

## 31. Tích hợp cảnh báo công nợ Google Drive

### 31.1. Mục tiêu và kiến trúc

PVT có thể hiển thị cùng dữ liệu cảnh báo công nợ mà Apps Script đang gửi Gmail.
Máy chủ Windows thường chỉ bật sau 08:00, vì vậy không dùng phương án Apps Script
đẩy dữ liệu vào PVT lúc 07:00–08:00. Luồng chính thức là:

```text
PVT khởi động/manager bấm quét
        ↓ HTTPS POST ra ngoài
Apps Script Web App chạy bằng tài khoản Google chủ sở hữu
        ↓ đọc quyền Drive sẵn có
File master + Google Sheets công nợ nhân viên
        ↓ JSON
PVT kiểm tra, lưu snapshot MySQL và hiển thị theo account
```

Cloudflare Tunnel không tham gia luồng này. Không mở port, không tạo API nhận dữ
liệu công nợ tại domain PVT. Máy chủ chỉ cần truy cập Internet ra
`https://script.google.com`.

### 31.2. Bảo toàn công cụ Gmail cũ

Hàm `masterCheckAllFilesAndSendEmail()` và trigger 07:00–08:00 hiện có phải được
giữ nguyên. Mã mới nằm riêng tại:

```text
integration/google-apps-script/pvt-debt-webapp-addon.gs
```

File bổ sung không có `MailApp.sendEmail()`, không gọi hàm Gmail cũ và không dùng
chung `isHoliday()` cũ vốn phụ thuộc active spreadsheet. Hàm mới mở file master
bằng `PVT_MASTER_SPREADSHEET_ID` và dùng helper có prefix `pvt..._` để tránh đụng
tên. Việc thêm tính năng PVT không thay đổi email/người nhận/lịch trigger cũ.

### 31.3. Cấu hình sheet master và quyền

Sheet `Danh sach nhan vien`:

- B1: số ngày cảnh báo T+ như logic Gmail hiện tại.
- A từ hàng 4: họ tên.
- B: ID hoặc URL file Google Sheets công nợ.
- C: email nhận cảnh báo; tiếp tục phục vụ hàm Gmail cũ.
- D: Bravo ID, phải bằng `account.account_name` trong PVT.

Không dùng tên hiển thị để mapping vì tên có thể trùng. `accountName`/Bravo ID
mới là khóa ổn định. Tài khoản Google deploy Web App phải có quyền đọc file
master và mọi file nhân viên; chia sẻ một thư mục Drive chứa toàn bộ file là đủ
nếu file con kế thừa quyền.

### 31.4. Hợp đồng Apps Script Web App

PVT POST JSON:

```json
{"action":"scanDebtAlerts","token":"shared-secret"}
```

Web App trả JSON gồm:

- `success`, `skipped`, `scanDate`, `generatedAt`, `daysOffset`, `message`,
  `skipReason`.
- `employees[]`: `accountName`, `employeeName`, ID/URL file, sheet/đợt/tháng báo
  cáo, `status`, `message`, `alerts[]`.
- `alerts[]`: mục, TT, nhà cung cấp, hóa đơn, trị giá hiển thị, ngày đến hạn ISO
  `yyyy-MM-dd`, T+.

Web App giữ logic chọn sheet mới nhất, phát hiện trùng đợt/tháng, chỉ quét mục II
và III, đọc B1 và bỏ qua Chủ Nhật/ngày trong `Lich nghi le`. Nó không gửi email.

Web App deploy **Execute as Me**, access **Anyone** và dùng URL `/exec`. Endpoint
công khai về mặt mạng nhưng mọi request phải có token. Token chỉ nằm trong Apps
Script Script Properties và file cấu hình ngoài JAR, không commit Git/log/chụp
ảnh chia sẻ.

### 31.5. Cấu hình PVT

Cấu hình mặc định trong JAR là tắt để bản cũ và các site chưa chuẩn bị Google
không bị ảnh hưởng:

```properties
app.debt-alert.enabled=false
app.debt-alert.web-app-url=
app.debt-alert.shared-token=
app.debt-alert.connect-timeout-seconds=10
app.debt-alert.read-timeout-seconds=330
app.debt-alert.startup-delay-ms=3000
app.debt-alert.retry-delay-ms=900000
app.debt-alert.earliest-sync-hour=7
app.debt-alert.task-deadline-hour=16
```

Site muốn dùng phải ghi đè ba dòng đầu trong `application.properties` cạnh JAR.
URL chỉ chấp nhận HTTPS host `script.google.com`; token không được log.

### 31.6. Đồng bộ và xử lý lỗi

- Sau khi Spring khởi động khoảng 3 giây, lượt đầu luôn quét lại kể cả database
  đã có SUCCESS hôm nay. Coordinator chạy thread nền nên không giữ/chậm request.
- Nếu lỗi mạng, JSON, quyền Drive hoặc Apps Script, trạng thái `FAILED` được lưu
  và tự thử lại sau 15 phút.
- `SUCCESS` hoặc `SKIPPED` (Chủ Nhật/ngày nghỉ) kết thúc quét tự động trong ngày.
- Manager có route POST `/home/manager/debt-alerts/sync` để ép quét lại.
- Atomic guard chặn hai lượt quét đồng thời trong JVM; Apps Script còn dùng
  `ScriptLock` để chặn request đồng thời phía Google.
- Snapshot trả về phải đúng ngày hiện tại. Payload giới hạn 500 nhân viên và
  10.000 hồ sơ để JSON lỗi không làm đầy database.
- Field null/trắng/chuỗi quá dài được chuẩn hóa trước khi lưu. Ngày sai không làm
  sập trang. Bravo ID thiếu hoặc không tồn tại được đánh dấu cho manager sửa.
- Apps Script `ContentService` trả HTTP 302 tới
  `script.googleusercontent.com`; client chỉ đi theo redirect HTTPS đúng host
  Google này rồi đọc JSON, từ chối redirect sang host khác.

### 31.6.1. Tự tạo lịch thanh toán

Sau khi snapshot được lưu thành công, PVT tạo đúng một `MonthEvent` cá nhân cho
mỗi account có hóa đơn cảnh báo. Công việc có deadline mặc định 16:00 trong ngày,
`monthEventStatus=0`, hiển thị và không phải việc Cả phòng. Hai cột metadata mới:

- `automation_source=DEBT_ALERT`.
- `automation_date`: ngày quét.

Nhờ hai cột này, quét lại/manager bấm quét nhiều lần trong cùng ngày chỉ cập nhật
nội dung, không tạo trùng. Nếu lượt quét OK xác nhận account không còn cảnh báo,
công việc tự sinh hôm đó được xóa mềm. Nếu file Google đang lỗi/DUPLICATE thì giữ
lịch cũ, không xóa theo dữ liệu không tin cậy. Lịch đã bị manager xóa mềm không
được tự phục hồi khi quét lại.

Account không có hóa đơn cảnh báo ngay từ đầu sẽ không được tạo `MonthEvent`.

Nội dung gom hóa đơn theo số ngày còn lại và sắp xếp tăng dần, ví dụ:

```text
Thanh toán T+4: Mai Lê (599), XYZ (456), T+5: Yuefa (582)
```

Hóa đơn quá hạn đứng trước và ghi `Quá hạn N ngày`. PVT tự tính T+ lại từ
`dueDate` thay vì phụ thuộc hoàn toàn field JSON, tránh null/khác quy ước tên.
Chu kỳ 15 phút chỉ là retry sau lỗi; không phải delay của lượt khởi động.

### 31.7. Database và phân quyền hiển thị

Hibernate `ddl-auto=update` tạo:

- `debt_alert_sync`: một row/ngày, trạng thái RUNNING/SUCCESS/SKIPPED/FAILED,
  thời điểm thử/thành công và tổng cảnh báo.
- `debt_alert_employee`: snapshot từng nhân viên/file/report.
- `debt_alert_item`: từng hồ sơ công nợ thuộc snapshot nhân viên.

Resync cùng ngày thay danh sách con trong snapshot, không cộng trùng dữ liệu.
Employee chỉ được lọc đúng `Principal.getName()`. Manager thấy toàn bộ nhân viên,
gồm lỗi `MISSING_ACCOUNT`, `ACCOUNT_NOT_FOUND`, `DUPLICATE`, `NO_REPORT`, `ERROR`.
Route trang là `GET /home/employee/debt-alerts`; security hiện tại cho cả employee
và manager vào `/home/employee/**`, còn POST ép quét nằm dưới `/home/manager/**`.

### 31.8. Giao diện

Khi `app.debt-alert.enabled=true`:

- Nav có mục **Cảnh báo công nợ**.
- Home có thẻ tóm tắt trạng thái/số hồ sơ.
- Trang chi tiết có bảng cùng các cột email: Mục, TT, Nhà cung cấp, Số hóa đơn,
  Trị giá, Ngày đến hạn, T+.
- Manager có nút **Quét lại Google Drive** và thấy liên kết file/diagnostic mọi
  nhân viên.
- Khi đang quét, trang tự reload 8 giây/lần; gọi Google không chặn request web.

Khi cấu hình tắt, nav/thẻ Home biến mất và scheduler không gọi Google.

### 31.9. File code quan trọng

```text
config/DebtAlertProperties.java
config/DebtAlertHttpConfig.java
config/DebtAlertViewModelAdvice.java
controller/DebtAlertController.java
model/debt/*
model/dto/debt/*
repository/debt/IDebtAlertSyncRepository.java
service/debt/DebtAlertAppsScriptClient.java
service/debt/DebtAlertStoreService.java
service/debt/DebtAlertSyncService.java
service/debt/DebtAlertSyncCoordinator.java
service/debt/DebtAlertTaskService.java
templates/debtAlerts.html
integration/google-apps-script/*
```

Hướng dẫn thao tác từng bước nằm trong
`integration/google-apps-script/README.md`.

### 31.10. Rollback và nghiệm thu

Backup trước module:

- `build/libs/pvt-v2.0-before-debt.jar`.
- `build/backups/pvt-v2.0-before-debt-source.zip`.

Rollback nhanh riêng chức năng: đặt `app.debt-alert.enabled=false`, restart PVT;
Gmail cũ không bị ảnh hưởng. Các bảng mới để lại không tác động module khác.

JAR tích hợp được xuất đồng thời dưới hai tên có cùng nội dung/hash:

- `build/libs/pvt.jar`: tên dùng để chạy/cập nhật máy chủ.
- `build/libs/pvt-v2.2-debt-auto-task.jar`: bản hiện tại, có tự tạo lịch.
- `build/libs/pvt-v2.1-before-auto-task.jar`: rollback ngay trước tính năng tạo lịch.

Nghiệm thu tối thiểu:

1. Trigger cũ vẫn trỏ `masterCheckAllFilesAndSendEmail` và email chạy như trước.
2. Hàm test Apps Script trả JSON thành công nhưng không gửi email.
3. D có Bravo ID khớp PVT; manager thấy lỗi rõ nếu thiếu/sai.
4. PVT tự quét sau startup, không treo trang trong lúc Google xử lý.
5. Employee không thấy dữ liệu account khác; manager thấy tất cả.
6. Chủ Nhật/ngày lễ hiển thị SKIPPED; lỗi mạng hiển thị FAILED và tự retry.
7. Nút quét lại không tạo hai job đồng thời và resync không nhân đôi item.

### 31.11. Lưu ý cập nhật JAR trên Windows

Phải tắt PVT bằng tray icon và xác nhận không còn tiến trình Java của đúng JAR
trước khi chép đè `pvt.jar`. Không được chép đè JAR khi Java đang chạy: classloader
có thể đọc lẫn file cũ/mới, gây `NoClassDefFoundError`, login POST 405 và shutdown
không sạch. Sau đó chỉ chạy shortcut một lần. `run.vbs` mới kiểm tra command line
của `java.exe/javaw.exe` và không tạo tiến trình thứ hai cho cùng đường dẫn JAR.

Nếu log có `Port 80 was already in use`, đang có PVT/ứng dụng khác chiếm port;
không tiếp tục bấm shortcut. Tray icon mở URL trong `app.public-url`; cấu hình Đà
Nẵng nên là `https://othpdn.phongvattu.online` để tránh mở bản local HTTP bị trình
duyệt ghi “Không bảo mật”.

## 32. Giao diện phản hồi dạng chat — text và ảnh đính kèm

### 32.1. Phạm vi an toàn

Giao diện trao đổi của một công việc đã chuyển sang dạng chat khi bật feature flag.
Bảng `response_event_information`, controller POST chính, validation, cập nhật
`responseStatus` và SSE cũ vẫn tiếp tục được dùng để giữ tương thích với logic cũ.

Từ bản v2.4, chat hỗ trợ gửi text kèm ảnh hoặc gửi ảnh không kèm text. Ảnh không
được lưu trực tiếp trong bảng phản hồi cũ; metadata ảnh nằm ở bảng riêng
`response_attachment`, còn file ảnh nằm ngoài database trong thư mục cấu hình.
Thiết kế này giúp rollback JAR cũ ít rủi ro: JAR cũ bỏ qua bảng mới, text phản hồi
cũ vẫn còn nguyên.

### 32.2. Feature flag và rollback tức thì

```properties
app.response-chat.enabled=false
app.response-chat.attachment-folder-path=./response-attachments
app.response-chat.max-images-per-message=5
app.response-chat.max-image-size-bytes=10485760
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=30MB
```

- `false`: controller trả template nguyên bản `eventResponse.html` với table và
  modal **+ Thêm phản hồi**.
- `true`: controller trả template mới `eventResponseChat.html`.

Flag mặc định trong JAR là false. Muốn thử tại một site phải thêm
`app.response-chat.enabled=true` vào `application.properties` cạnh JAR rồi
restart. Nếu UX có vấn đề, đổi lại false và restart để quay về template bảng cũ.
Các bảng/file ảnh mới sẽ không được JAR cũ sử dụng, nhưng cũng không chặn JAR cũ chạy.

Template cũ không bị sửa hoặc xóa, là đường rollback tại chỗ. Mốc trước chat:

- `build/libs/pvt-v2.2-before-response-chat.jar`.
- `build/backups/pvt-v2.2-before-response-chat-source.zip`.

JAR text-chat hiện tại:

- `build/libs/pvt.jar` — tên triển khai.
- `build/libs/pvt-v2.3-response-chat-text.jar` — tên lưu phiên bản.

Mốc trước khi thêm ảnh:

- `build/libs/pvt-v2.3-before-response-images.jar`.
- `build/backups/pvt-v2.3-before-response-images-source.zip`.

JAR chat text + ảnh:

- `build/libs/pvt.jar` — tên triển khai.
- `build/libs/pvt-v2.4-response-chat-images.jar` — tên lưu phiên bản.
- SHA256 v2.4: `5BE8F60B3028D03E34A4E8440D97895382B4AFBB6B8E84F1C2668FFD74E562DD`.
- `build/libs/pvt-v2.4.1-response-chat-ux-polish.jar` — bản chỉnh UX chat:
  tự cuộn xuống vùng nhập, ảnh mở lightbox trong trang, layout PC rộng hơn.
- SHA256 v2.4.1: `AB807D4B59FEB254653565A8FE1099E3F1A41777C89CD661FA24A2F70C9D3E79`.
- `build/libs/pvt-v2.4.2-response-chat-scroll-upload-fix.jar` — bản sửa khung chat
  dài không xuống đúng đáy sau khi ảnh load, bỏ cuộn trang ngoài khi mở chat và
  tối ưu ảnh lớn phía trình duyệt trước khi upload.
- SHA256 v2.4.2: `7AD4BD69AE95815BE076AC3A75DDBD231F2304F67C3E194E816BA22CF6A74236`.
- `build/libs/pvt-v2.4.3-response-chat-mobile-actions.jar` — bản chỉnh cụm nút
  action trên mobile, rút gọn “Trở lại tab cũ” thành “Tab cũ” để không rơi xuống
  hàng thứ hai.
- SHA256 v2.4.3: `C02FBC68575541CF69DB6677BBD87E0DC63386ABB2FA88A0657C05F25E997B1C`.

### 32.3. Hành vi giao diện chat

- Header công việc và các action manager vẫn giữ.
- Employee không còn thấy action manager trên trang chat.
- Tin của account đang đăng nhập nằm phải/màu xanh; tin khác nằm trái/màu trắng.
- Danh sách đổi từ thứ tự mới nhất trước thành trình tự hội thoại cũ → mới và tự
  cuộn xuống cuối khi vừa mở/gửi.
- Textarea luôn nằm dưới cùng; Enter gửi, Shift+Enter xuống dòng; xử lý trạng thái
  gõ tiếng Việt IME để không gửi nhầm khi đang ghép dấu.
- Nút gửi disabled khi không có text và không có ảnh. Vì vậy không còn cảnh báo
  browser kiểu “Vui lòng điền vào trường này”.
- Nút camera/kẹp ảnh cho phép chọn nhiều ảnh, preview trước khi gửi. Mặc định tối đa
  5 ảnh/lần gửi, 10MB/ảnh. Cho phép `jpg`, `jpeg`, `png`, `gif`, `webp`, `bmp`.
- Có thể gửi ảnh không kèm chữ; khi hiển thị, ảnh là thumbnail trong bong bóng chat
  và bấm vào sẽ mở lightbox ngay trong trang. Lightbox có nút đóng, hỗ trợ ESC,
  click nền tối, nút back của trình duyệt và vẫn có link “Mở ảnh ở tab mới”.
- Khi mở trang chat, không cuộn toàn bộ trang ngoài từ đầu xuống nữa. Thay vào đó,
  danh sách tin nhắn bên trong tự ghim xuống đáy thật; sau khi ảnh trong chat load
  xong sẽ tự chỉnh lại scroll để không bị đứng lưng chừng.
- Trên PC, khung chat dùng chiều rộng lớn hơn, chiều cao danh sách tính theo viewport
  và ảnh tự chia 1/2/3 cột tùy số lượng; tránh cảm giác layout tablet trên desktop.
- Khi gửi ảnh lớn, trình duyệt tự resize/nén ảnh trước khi upload. Ảnh nhỏ và GIF
  giữ nguyên để tránh mất chất lượng/animation.
- Gửi AJAX giữ nguyên nội dung nếu lỗi; chặn double-submit và tự tải lại sau lưu.
- SSE cũ tiếp tục báo refresh. Khi người dùng đang đọc tin cũ, refresh không ép
  cuộn xuống nếu vị trí hiện tại cách xa cuối danh sách.
- Nội dung từ REST được đưa vào DOM bằng `textContent` sau khi giải mã HTML entity,
  không dựng raw HTML từ dữ liệu phản hồi cũ.

### 32.4. File liên quan

```text
config/ResponseChatProperties.java
controller/EventController.java
model/event/ResponseAttachment.java
model/dto/ResponseAttachmentView.java
model/dto/ResponseChatMessageView.java
repository/event/IResponseAttachmentRepository.java
service/response/ResponseAttachmentStorageService.java
templates/eventResponseChat.html
templates/eventResponse.html             # giữ nguyên làm legacy fallback
src/test/java/com/hoaphat/pvt/controller/ResponseChatPageIntegrationTest.java
```

### 32.5. Lưu ảnh phản hồi và dữ liệu database

Ảnh phản hồi dùng entity/table mới `ResponseAttachment` / `response_attachment`.
Mỗi ảnh thuộc về một `ResponseEventInformation` qua `response_id`. Quan hệ có
`ON DELETE CASCADE`, nên khi phản hồi bị xóa cứng thì metadata ảnh cũng bị xóa theo.

Các trường chính:

- `idAttachment`: khóa chính.
- `response_id`: phản hồi cha.
- `originalFileName`: tên file người dùng gửi.
- `storedRelativePath`: đường dẫn tương đối trong thư mục attachment.
- `contentType`, `fileSize`, `createdByUser`, `createdByDate`.

File ảnh nằm ngoài MySQL tại:

```properties
app.response-chat.attachment-folder-path=./response-attachments
```

Khi chạy bằng `run.vbs`, thư mục tương đối `./response-attachments` nằm cạnh JAR vì
`run.vbs` đã đặt `WshShell.CurrentDirectory = strPath`. Nếu máy chủ muốn rõ ràng hơn,
nên cấu hình đường dẫn tuyệt đối, ví dụ:

```properties
app.response-chat.attachment-folder-path=D:/PVT/response-attachments
```

Đường mở ảnh là controller có đăng nhập:

```text
/home/employee/private/response-attachment/{id}
```

Không dùng `/img-local/**` cho ảnh phản hồi vì `/img-local/**` đang được Spring
Security bỏ qua để cache thư viện ảnh/video chung. Ảnh phản hồi là dữ liệu theo công
việc nên phải đi qua route cần đăng nhập.

Khi quản lý xóa hẳn công việc trong Thư mục đã xóa hoặc xóa tài khoản, service gọi
`ResponseAttachmentStorageService.deleteFilesByMonthEventId(...)` để dọn file vật lý
trước khi xóa phản hồi. Nếu file đang bị Windows khóa, DB vẫn xóa theo luồng cũ; file
có thể còn rác và dọn tay sau.

Rollback:

- Đổi `app.response-chat.enabled=false` và restart để quay lại giao diện bảng cũ.
- Rollback JAR v2.3/v2.2 vẫn chạy được vì bảng mới không thay đổi bảng phản hồi cũ.
- Nếu muốn dọn sạch phần ảnh sau rollback lâu dài, backup trước rồi có thể xóa thư mục
  `response-attachments` và drop bảng `response_attachment`; không cần làm bước này
  để JAR cũ hoạt động.

## 33. v2.5 — Tổng hợp phản hồi nhân viên và thao tác hàng loạt

### 33.1. Mục đích và phạm vi

Trang Lịch cần làm của một tab nhân viên có thêm dòng ảo STT 1:

```text
Tổng hợp phản hồi của {tên hiển thị nhân viên} ngày hôm nay
```

Dòng này không phải `month_event`, không tạo record database và không có ngày giao
hoặc deadline. Dòng luôn xuất hiện ở tab của một nhân viên, kể cả hôm nay chưa có
phản hồi, để người dùng vẫn đổi sang mốc hôm trước. Tab Tất cả/Cả phòng không có
dòng tổng hợp. Các công việc thật bắt đầu từ STT 2 khi dòng tổng hợp hiện diện.

Manager và employee đều được xem. Employee luôn bị backend ép xem account đang
đăng nhập dù tự sửa `accountName` trên URL. Manager được chọn account bất kỳ còn
tồn tại và có thêm thao tác đổi deadline/xóa mềm nhiều mục.

### 33.2. Bốn mốc thời gian

Mọi mốc dùng `Clock` của ứng dụng với timezone `Asia/Ho_Chi_Minh` và cận cuối
độc quyền để không đếm trùng:

- `TODAY`: từ 00:00 hôm nay đến thời điểm mở trang.
- `YESTERDAY`: trọn ngày lịch trước, từ 00:00 hôm trước đến trước 00:00 hôm nay.
- `MORNING`: từ 00:00 đến trước 12:00; nếu mở trước 12:00 thì kết thúc tại thời
  điểm mở trang.
- `AFTERNOON`: từ 12:00 đến thời điểm mở trang; trước 12:00 thì danh sách rỗng.

Chỉ lấy phản hồi có `createdByUser` đúng nhân viên đang tổng hợp. Công việc đã ẩn
hoặc đã xóa mềm không xuất hiện. Các phản hồi được nhóm theo công việc và sắp theo
thời gian tăng dần. Phản hồi chỉ có file vẫn hiển thị tên/link file, không bị xem
như dòng trống. Nút **Mở trao đổi** đi tới đúng khung chat hiện có.

### 33.3. Ưu tiên theo đúng ngày lịch sử

Bảng ưu tiên v2.0 vốn đã có `daily_priority_selection.priority_date`; qua 0h ứng
dụng chỉ đọc ngày mới, không xóa ưu tiên cũ. Vì vậy:

- Hôm nay/Từ sáng/Từ chiều đọc ưu tiên của ngày hiện tại.
- Hôm trước đọc ưu tiên của ngày hôm trước.
- Tiêu đề công việc hiển thị `(1)` đến `(7)` màu đỏ trước nội dung nếu có.
- Việc Cả phòng không có ưu tiên.

Khi quản lý đổi người phụ trách của một việc, code chỉ xóa `task_priority` của
ngày hiện tại (`deleteTodayForEvent`) và giữ lịch sử ngày trước. Xóa cứng công việc
hoặc tài khoản vẫn dọn toàn bộ lịch sử liên quan. Chi tiết quyết định nằm tại
`docs/decisions/ADR-001-response-summary-priority-history.md`.

### 33.4. Đổi deadline nhiều mục

Chỉ manager thấy checkbox và nút **Đổi deadline nhiều mục**. Công việc Cả phòng
vẫn hiển thị trong tổng hợp nhưng checkbox disabled; backend cũng từ chối nếu cố
gửi ID đó thủ công.

Các lựa chọn:

- `1 ngày`, `2 ngày`, `3 ngày`: cộng ngày lịch và giữ nguyên giờ/phút.
- `1 buổi`: deadline trước 12:00 chuyển thành 16:00 cùng ngày; deadline từ 12:00
  trở đi chuyển thành 11:00 ngày kế tiếp.

Modal hiển thị nội dung, deadline hiện tại và deadline mới trước khi lưu. Backend
đối chiếu lại toàn bộ ID với đúng account + mốc tổng hợp, khóa từng `month_event`,
kiểm tra tất cả trước khi ghi, rồi mới cập nhật trong một transaction. Mỗi việc
được tạo phản hồi `Báo cáo lại HH:mm dd/MM/yyyy` bằng logic dùng chung hiện có.
Nếu bất kỳ mục nào không hợp lệ thì cả batch rollback. Sau khi thành công chỉ gửi
một tín hiệu SSE refresh.

### 33.5. Xóa nhiều mục

Nút **Quản lý xóa nhiều mục** có modal liệt kê lại các việc đã chọn. Thao tác chỉ
đặt `is_deleted=true`, giữ toàn bộ phản hồi và attachment để có thể khôi phục từ
Thư mục đã xóa. Validation, khóa row và transaction áp dụng giống đổi deadline;
không có trường hợp mục đầu bị xóa còn mục sau báo lỗi.

### 33.6. Route và phân quyền

```text
GET  /home/employee/response-summary
POST /home/manager/response-summary/shift-deadlines
POST /home/manager/response-summary/delete
```

- GET: `ROLE_EMPLOYEE` và `ROLE_MANAGER`.
- Hai POST: chỉ `ROLE_MANAGER`, có CSRF theo cấu hình Spring Security hiện tại.
- Nội dung phản hồi/tên công việc được xuất bằng `th:text`; dữ liệu dựng bằng JS
  được escape trước khi đưa vào HTML.

### 33.7. File chính và kiểm thử

```text
controller/ResponseSummaryController.java
model/dto/ResponseSummary*.java
model/dto/DeadlineShiftOption.java
repository/event/IResponseEventInformationRepository.java
service/response/ResponseSummaryService.java
service/response/ResponseSummaryActionService.java
service/monthEvent/MonthEventService.java
service/priority/WorkPriorityService.java
templates/private.html
templates/responseSummary.html
```

Các test chính:

```text
ResponseSummaryPeriodTest
ResponseSummaryServiceTest
ResponseSummaryActionServiceTest
ResponseSummaryPageIntegrationTest
DeadlineShiftOptionTest
MonthEventServiceTest
WorkPriorityServiceTest
```

### 33.8. Database, triển khai và rollback

Tính năng này **không tạo bảng/cột mới**. Không cần chạy SQL migration và không
thay đổi `application.properties`. Trước khi phát triển đã tạo mốc:

```text
build/libs/pvt-before-response-summary-2026-07-30.jar
build/backups/pvt-before-response-summary-2026-07-30-source.zip
```

JAR đã build và kiểm thử:

```text
build/libs/pvt.jar
build/libs/pvt-v2.5-response-summary.jar
SHA-256: C313B4FE80C1AFCD7968507CE251AC5B5C80730AE767E882E4DF5116D3000191
```

Hai file JAR trên là cùng một nội dung; `pvt.jar` là tên chép sang máy chủ, còn
`pvt-v2.5-response-summary.jar` là tên lưu phiên bản. Lần build 31/07/2026 chạy
toàn bộ 90 test, kết quả 0 failure.

Quy trình triển khai giữ nguyên: backup database, tắt PVT bằng tray icon, xác nhận
không còn tiến trình Java của JAR, chép JAR mới thành `pvt.jar`, chạy shortcut một
lần rồi kiểm tra log/login/tab nhân viên. Nếu cần rollback, tắt bản mới và chép
lại JAR backup trên; database không cần rollback vì release không đổi schema.
