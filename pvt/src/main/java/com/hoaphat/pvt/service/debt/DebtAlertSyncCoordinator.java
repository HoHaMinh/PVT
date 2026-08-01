package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DebtAlertSyncCoordinator {
    private final DebtAlertProperties properties;
    private final DebtAlertStoreService storeService;
    private final DebtAlertSyncService syncService;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean firstScheduledRun = new AtomicBoolean(true);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvt-debt-alert-sync");
        thread.setDaemon(true);
        return thread;
    });

    public DebtAlertSyncCoordinator(DebtAlertProperties properties,
                                    DebtAlertStoreService storeService,
                                    DebtAlertSyncService syncService,
                                    Clock clock) {
        this.properties = properties;
        this.storeService = storeService;
        this.syncService = syncService;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "${app.debt-alert.startup-delay-ms:10000}",
            fixedDelayString = "${app.debt-alert.retry-delay-ms:900000}")
    public void scheduledSync() {
        if (!properties.isConfigured()) {
            return;
        }
        // Mỗi lần mở PVT phải quét lại ngay cả khi database đã có SUCCESS hôm nay.
        // Các lần scheduler sau chỉ đóng vai trò retry nếu lượt đầu thất bại.
        if (firstScheduledRun.compareAndSet(true, false)) {
            trigger(true);
            return;
        }
        int earliestHour = Math.max(0, Math.min(23, properties.getEarliestSyncHour()));
        if (LocalTime.now(clock).isBefore(LocalTime.of(earliestHour, 0))) {
            return;
        }
        trigger(false);
    }

    public boolean trigger(boolean force) {
        if (!properties.isConfigured()) {
            return false;
        }
        if (!force && storeService.isCompletedToday()) {
            return false;
        }
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        executor.submit(() -> {
            try {
                syncService.synchronize();
            } finally {
                running.set(false);
            }
        });
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
