package com.hoaphat.pvt.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    // Danh sách các kết nối (SseEmitter) đang mở từ các trình duyệt.
    // Dùng CopyOnWriteArrayList để đảm bảo an toàn khi nhiều người dùng cùng lúc.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Cho phép một trình duyệt đăng ký "nghe" thông báo.
     * @return một đối tượng SseEmitter để duy trì kết nối.
     */
    public SseEmitter subscribe() {
        // Tạo một emitter với thời gian sống không giới hạn.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Xử lý khi kết nối hoàn thành, timeout hoặc lỗi -> xóa emitter khỏi danh sách.
        Runnable removeEmitter = () -> this.emitters.remove(emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError((e) -> removeEmitter.run());

        // Thêm emitter mới vào danh sách quản lý.
        this.emitters.add(emitter);

        return emitter;
    }

    /**
     * Gửi tín hiệu "refresh" đến TẤT CẢ các trình duyệt đang kết nối.
     * Trình duyệt nhận được tín hiệu này sẽ tự động gọi hàm loadData().
     */
    public void sendRefreshSignal() {
        // Lặp qua tất cả các emitters và gửi sự kiện.
        for (SseEmitter emitter : this.emitters) {
            try {
                // Gửi một sự kiện có tên là "refresh", dữ liệu là "update"
                emitter.send(SseEmitter.event().name("refresh").data("update"));
            } catch (IOException e) {
                // Nếu gửi lỗi (ví dụ: người dùng đã đóng tab), xóa emitter đó đi.
                this.emitters.remove(emitter);
            }
        }
    }
}