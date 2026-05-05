package com.hoaphat.pvt.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    // Danh sách các kết nối (SseEmitter) đang mở từ các trình duyệt.
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
     * Gửi tín hiệu "refresh" đến TẤT CẢ các trình duyệt khi có dữ liệu mới.
     */
    public void sendRefreshSignal() {
        for (SseEmitter emitter : this.emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data("update"));
            } catch (IOException e) {
                this.emitters.remove(emitter);
            }
        }
    }

    /**
     * 🔥 Gửi tín hiệu duy trì (Heartbeat) định kỳ mỗi 20 giây.
     * Việc này giúp Cloudflare không ngắt kết nối do "im lặng" quá 100 giây.
     */
    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        for (SseEmitter emitter : this.emitters) {
            try {
                // Gửi sự kiện tên là "ping" để giữ kết nối sống.
                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (IOException e) {
                // Nếu trình duyệt đã đóng, xóa emitter để giải phóng bộ nhớ.
                this.emitters.remove(emitter);
            }
        }
    }
}