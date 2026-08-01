package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.model.dto.debt.DebtAlertApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DebtAlertSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebtAlertSyncService.class);

    private final DebtAlertAppsScriptClient client;
    private final DebtAlertStoreService storeService;
    private final DebtAlertTaskService taskService;

    public DebtAlertSyncService(DebtAlertAppsScriptClient client,
                                DebtAlertStoreService storeService,
                                DebtAlertTaskService taskService) {
        this.client = client;
        this.storeService = storeService;
        this.taskService = taskService;
    }

    public boolean synchronize() {
        storeService.markRunning();
        try {
            DebtAlertApiResponse response = client.scan();
            if (response == null || !response.isSuccess()) {
                String message = response == null ? null : response.getMessage();
                throw new IllegalStateException(message == null || message.trim().isEmpty()
                        ? "Apps Script báo quét dữ liệu không thành công." : message);
            }
            storeService.saveResponse(response);
            if (!response.isSkipped()) {
                taskService.synchronizeDailyTasks(response);
            }
            LOGGER.info("Đồng bộ cảnh báo công nợ thành công.");
            return true;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            storeService.saveFailure(message);
            LOGGER.error("Không thể đồng bộ cảnh báo công nợ: {}", message, e);
            return false;
        }
    }
}
