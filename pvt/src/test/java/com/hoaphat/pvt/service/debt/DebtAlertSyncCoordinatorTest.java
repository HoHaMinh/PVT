package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.mockito.Mockito.*;

class DebtAlertSyncCoordinatorTest {
    private DebtAlertSyncCoordinator coordinator;

    @AfterEach
    void tearDown() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
    }

    @Test
    void firstScheduledRunAlwaysScansImmediatelyEvenIfTodayPreviouslySucceeded() {
        DebtAlertProperties properties = new DebtAlertProperties();
        properties.setEnabled(true);
        properties.setWebAppUrl("https://script.google.com/macros/s/test/exec");
        properties.setSharedToken("secret");
        DebtAlertStoreService storeService = mock(DebtAlertStoreService.class);
        DebtAlertSyncService syncService = mock(DebtAlertSyncService.class);
        when(storeService.isCompletedToday()).thenReturn(true);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 7, 8, 0, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        coordinator = new DebtAlertSyncCoordinator(properties, storeService, syncService, clock);

        coordinator.scheduledSync();

        verify(syncService, timeout(1000).times(1)).synchronize();
        verify(storeService, never()).isCompletedToday();
    }
}
