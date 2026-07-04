package com.hoaphat.pvt.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L;

    // Danh sách các kết nối (SseEmitter) đang mở từ các trình duyệt.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Mọi lần gửi SSE chạy tuần tự trên một daemon thread, không giữ request POST.
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvt-sse-broadcast");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Cho phép một trình duyệt đăng ký "nghe" thông báo.
     * @return một đối tượng SseEmitter để duy trì kết nối.
     */
    public SseEmitter subscribe() {
        // Recycle kết nối định kỳ để loại emitter cũ; EventSource sẽ tự reconnect.
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        // Xử lý khi kết nối hoàn thành, timeout hoặc lỗi -> xóa emitter khỏi danh sách.
        Runnable removeEmitter = () -> this.emitters.remove(emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(() -> {
            removeEmitter.run();
            safelyComplete(emitter);
        });
        emitter.onError((e) -> removeEmitter.run());

        // Thêm emitter mới vào danh sách quản lý.
        this.emitters.add(emitter);

        // Commit response SSE ngay, tránh proxy giữ request ở trạng thái chưa có dữ liệu.
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .reconnectTime(3000)
                    .data("ok"));
        } catch (IOException | IllegalStateException e) {
            removeEmitter.run();
            safelyComplete(emitter);
        }

        return emitter;
    }

    /**
     * Gửi tín hiệu "refresh" đến TẤT CẢ các trình duyệt khi có dữ liệu mới.
     */
    public void sendRefreshSignal() {
        sendAsync("refresh", "update");
    }

    /**
     * 🔥 Gửi tín hiệu duy trì (Heartbeat) định kỳ mỗi 20 giây.
     * Việc này giúp Cloudflare không ngắt kết nối do "im lặng" quá 100 giây.
     */
    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        sendAsync("ping", "heartbeat");
    }

    private void sendAsync(String eventName, String data) {
        try {
            notificationExecutor.execute(() -> sendToAll(eventName, data));
        } catch (RejectedExecutionException ignored) {
            // Ứng dụng đang tắt, không nhận thêm tác vụ gửi SSE.
        }
    }

    private void sendToAll(String eventName, String data) {
        for (SseEmitter emitter : this.emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                this.emitters.remove(emitter);
                safelyComplete(emitter);
            }
        }
    }

    private void safelyComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // Async request đã được container hoàn tất.
        }
    }

    @PreDestroy
    public void shutdown() {
        notificationExecutor.shutdownNow();
        for (SseEmitter emitter : emitters) {
            safelyComplete(emitter);
        }
        emitters.clear();
    }
}
