package com.hoaphat.pvt.service.reminder;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.reminder.PersonalDailyReminder;
import com.hoaphat.pvt.model.reminder.PersonalReminderView;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.reminder.IPersonalDailyReminderRepository;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import com.hoaphat.pvt.service.price.PriceLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDailyReminderServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock private IPersonalDailyReminderRepository reminderRepository;
    @Mock private IAccountRepository accountRepository;
    @Mock private PriceLookupService priceLookupService;
    @Mock private DebtAlertStoreService debtAlertStoreService;

    private PersonalDailyReminderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 10, 15, 8, 5, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new PersonalDailyReminderService(reminderRepository, accountRepository,
                priceLookupService, debtAlertStoreService, clock);
    }

    @Test
    void dailyScanStoresOwnFlagsForEveryAccountOnlyOnce() {
        when(reminderRepository.existsByReminderDate(TODAY)).thenReturn(false);
        when(accountRepository.findAll()).thenReturn(Arrays.asList(
                new Account("employee.one", "pw", "Nhân viên Một"),
                new Account("employee.two", "pw", "Nhân viên Hai"),
                new Account("manager", "pw", "Quản lý")));
        when(priceLookupService.getPendingAccountNames()).thenReturn(
                new HashSet<>(Arrays.asList("employee.one", "manager")));
        when(debtAlertStoreService.getPendingImportAlertAccountNamesToday()).thenReturn(
                Collections.singleton("employee.two"));

        assertTrue(service.scanToday());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PersonalDailyReminder>> captor = ArgumentCaptor.forClass(List.class);
        verify(reminderRepository).saveAll(captor.capture());
        List<PersonalDailyReminder> rows = captor.getValue();
        assertEquals(3, rows.size());
        assertReminder(rows, "employee.one", true, false);
        assertReminder(rows, "employee.two", false, true);
        assertReminder(rows, "manager", true, false);
        verify(reminderRepository).deleteByReminderDateBefore(TODAY.minusDays(90));

        when(reminderRepository.existsByReminderDate(TODAY)).thenReturn(true);
        assertFalse(service.scanToday());
        verify(reminderRepository, times(1)).saveAll(any());
    }

    @Test
    void viewCanClearResolvedSnapshotButCannotAddItemsCreatedAfterScan() {
        PersonalDailyReminder snapshot = new PersonalDailyReminder();
        snapshot.setReminderDate(TODAY);
        snapshot.setAccountName("employee.one");
        snapshot.setPriceLookupPending(true);
        snapshot.setImportAlertPending(false);
        when(reminderRepository.findByReminderDateAndAccountName(TODAY, "employee.one"))
                .thenReturn(Optional.of(snapshot));
        when(priceLookupService.hasPendingForAccount("employee.one")).thenReturn(false);
        PersonalReminderView view = service.getTodayForAccount("employee.one");

        assertFalse(view.isPriceLookupPending());
        assertFalse(view.isImportAlertPending());
        assertFalse(view.hasAny());
        assertFalse(snapshot.isPriceLookupPending());
        verify(reminderRepository).save(snapshot);
        // Cờ false không đọc nguồn hiện tại, nên nội dung mới trong ngày không thể tự bật.
        verifyNoInteractions(debtAlertStoreService);
    }

    @Test
    void accountWithoutTodaysSnapshotSeesNoReminder() {
        when(reminderRepository.findByReminderDateAndAccountName(TODAY, "employee.one"))
                .thenReturn(Optional.empty());

        PersonalReminderView view = service.getTodayForAccount("employee.one");

        assertFalse(view.hasAny());
        verifyNoInteractions(priceLookupService, debtAlertStoreService);
    }

    private void assertReminder(List<PersonalDailyReminder> rows, String accountName,
                                boolean pricePending, boolean importPending) {
        PersonalDailyReminder row = rows.stream()
                .filter(value -> accountName.equals(value.getAccountName()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(TODAY, row.getReminderDate());
        assertEquals(pricePending, row.isPriceLookupPending());
        assertEquals(importPending, row.isImportAlertPending());
        assertNotNull(row.getScannedAt());
    }
}
