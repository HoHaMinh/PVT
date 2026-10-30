package com.hoaphat.pvt.service.reminder;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PersonalReminderCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalReminderCoordinator.class);

    private final PersonalDailyReminderService reminderService;
    private final DebtAlertStoreService debtAlertStoreService;
    private final DebtAlertProperties debtAlertProperties;

    public PersonalReminderCoordinator(PersonalDailyReminderService reminderService,
                                       DebtAlertStoreService debtAlertStoreService,
                                       DebtAlertProperties debtAlertProperties) {
        this.reminderService = reminderService;
        this.debtAlertStoreService = debtAlertStoreService;
        this.debtAlertProperties = debtAlertProperties;
    }

    @Scheduled(initialDelayString = "${app.personal-reminder.startup-delay-ms:20000}",
            fixedDelayString = "${app.personal-reminder.retry-delay-ms:60000}")
    public void scheduledScan() {
        try {
            if (reminderService.isScannedToday()) return;
            // Nếu công nợ có cấu hình, phải đợi lượt quét Drive hôm nay hoàn tất
            // để không chụp thiếu cảnh báo NK rồi khóa snapshot cả ngày.
            if (debtAlertProperties.isConfigured()
                    && !debtAlertStoreService.isCompletedToday()) {
                return;
            }
            reminderService.scanToday();
        } catch (RuntimeException e) {
            LOGGER.error("Không thể tạo snapshot nhắc việc cá nhân đầu ngày.", e);
        }
    }
}
