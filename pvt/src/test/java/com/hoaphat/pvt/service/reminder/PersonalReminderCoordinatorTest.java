package com.hoaphat.pvt.service.reminder;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalReminderCoordinatorTest {
    @Mock private PersonalDailyReminderService reminderService;
    @Mock private DebtAlertStoreService debtAlertStoreService;

    private DebtAlertProperties properties;
    private PersonalReminderCoordinator coordinator;

    @BeforeEach
    void setUp() {
        properties = new DebtAlertProperties();
        coordinator = new PersonalReminderCoordinator(
                reminderService, debtAlertStoreService, properties);
    }

    @Test
    void waitsForConfiguredDebtScanBeforeTakingDailySnapshot() {
        properties.setEnabled(true);
        properties.setWebAppUrl("https://script.google.com/macros/s/test/exec");
        properties.setSharedToken("secret");
        when(reminderService.isScannedToday()).thenReturn(false);
        when(debtAlertStoreService.isCompletedToday()).thenReturn(false);

        coordinator.scheduledScan();

        verify(reminderService, never()).scanToday();
    }

    @Test
    void scansAfterDebtDataIsReady() {
        properties.setEnabled(true);
        properties.setWebAppUrl("https://script.google.com/macros/s/test/exec");
        properties.setSharedToken("secret");
        when(reminderService.isScannedToday()).thenReturn(false);
        when(debtAlertStoreService.isCompletedToday()).thenReturn(true);

        coordinator.scheduledScan();

        verify(reminderService).scanToday();
    }

    @Test
    void scansPriceRemindersWhenDebtFeatureIsNotConfigured() {
        when(reminderService.isScannedToday()).thenReturn(false);

        coordinator.scheduledScan();

        verify(reminderService).scanToday();
        verify(debtAlertStoreService, never()).isCompletedToday();
    }
}
