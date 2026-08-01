package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.debt.DebtAlertApiResponse;
import com.hoaphat.pvt.model.dto.debt.DebtAlertEmployeePayload;
import com.hoaphat.pvt.model.dto.debt.DebtAlertItemPayload;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtAlertTaskServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);
    private static final String ACCOUNT = "dminhhh";

    @Mock private IMonthEventRepository monthEventRepository;
    @Mock private IAccountRepository accountRepository;

    private DebtAlertTaskService service;
    private Account account;

    @BeforeEach
    void setUp() {
        DebtAlertProperties properties = new DebtAlertProperties();
        properties.setTaskDeadlineHour(16);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 7, 8, 30, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new DebtAlertTaskService(monthEventRepository, accountRepository,
                properties, clock);
        account = new Account(ACCOUNT, "password", "Hồ Hà Minh");
        lenient().when(accountRepository.findByAccountName(ACCOUNT)).thenReturn(account);
    }

    @Test
    void createsOnePersonalTaskGroupedAndSortedByTPlus() {
        when(monthEventRepository.findActiveAutomationTasksForAccount(
                ACCOUNT, DebtAlertTaskService.AUTOMATION_SOURCE))
                .thenReturn(Collections.emptyList());
        DebtAlertEmployeePayload employee = employee(
                item("Mai Lê", "599", "2026-07-11"),
                item("Yuefa", "582", "2026-07-12"),
                item("Samhwan Vina", "437", "2026-07-17"),
                item("XYZ", "456", "2026-07-11"));
        ArgumentCaptor<MonthEvent> captor = ArgumentCaptor.forClass(MonthEvent.class);

        int changed = service.synchronizeDailyTasks(response(employee));

        assertEquals(1, changed);
        verify(monthEventRepository).save(captor.capture());
        MonthEvent event = captor.getValue();
        assertEquals("Thanh toán T+4: Mai Lê (599), XYZ (456), "
                + "T+5: Yuefa (582), T+10: Samhwan Vina (437)",
                event.getMonthEventDescription());
        assertEquals(LocalDateTime.of(2026, 7, 7, 16, 0), event.getMonthEventDeadline());
        assertEquals(ACCOUNT, event.getAccount().getAccountName());
        assertEquals(0, event.getMonthEventStatus());
        assertFalse(event.getAll());
        assertFalse(event.getHidden());
        assertFalse(event.getDeleted());
        assertEquals("DEBT_ALERT", event.getAutomationSource());
        assertEquals(TODAY, event.getAutomationDate());
    }

    @Test
    void rescanningSameDayUpdatesExistingTaskInsteadOfCreatingDuplicate() {
        MonthEvent existing = new MonthEvent();
        existing.setMonthEventId(99);
        existing.setAccount(account);
        existing.setDeleted(false);
        when(monthEventRepository.findActiveAutomationTasksForAccount(
                ACCOUNT, DebtAlertTaskService.AUTOMATION_SOURCE))
                .thenReturn(Collections.singletonList(existing));

        service.synchronizeDailyTasks(response(employee(
                item("Mai Lê", "599", "2026-07-11"))));

        verify(monthEventRepository).save(existing);
        assertEquals(Integer.valueOf(99), existing.getMonthEventId());
        assertEquals("Thanh toán T+4: Mai Lê (599)", existing.getMonthEventDescription());
    }

    @Test
    void noAlertsSoftDeletesTodaysGeneratedTask() {
        MonthEvent existing = new MonthEvent();
        existing.setMonthEventId(99);
        existing.setAccount(account);
        existing.setDeleted(false);
        when(monthEventRepository.findActiveAutomationTasksForAccount(
                ACCOUNT, DebtAlertTaskService.AUTOMATION_SOURCE))
                .thenReturn(Collections.singletonList(existing));

        service.synchronizeDailyTasks(response(employee()));

        assertTrue(existing.getDeleted());
        verify(monthEventRepository).save(existing);
    }

    @Test
    void saturdayMorningDeadlineIsEleven() {
        service = serviceAt(2026, 7, 11, 8, 30);

        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 0),
                service.deadlineFor(LocalDate.of(2026, 7, 11),
                        LocalDateTime.of(2026, 7, 11, 8, 30)));
    }

    @Test
    void saturdayAfterElevenMovesDeadlineToNextMondayAfternoon() {
        service = serviceAt(2026, 7, 11, 11, 5);

        assertEquals(LocalDateTime.of(2026, 7, 13, 16, 0),
                service.deadlineFor(LocalDate.of(2026, 7, 11),
                        LocalDateTime.of(2026, 7, 11, 11, 5)));
    }

    @Test
    void sundayMovesDeadlineToNextMondayAfternoon() {
        service = serviceAt(2026, 7, 12, 9, 0);

        assertEquals(LocalDateTime.of(2026, 7, 13, 16, 0),
                service.deadlineFor(LocalDate.of(2026, 7, 12),
                        LocalDateTime.of(2026, 7, 12, 9, 0)));
    }

    private DebtAlertApiResponse response(DebtAlertEmployeePayload employee) {
        DebtAlertApiResponse response = new DebtAlertApiResponse();
        response.setSuccess(true);
        response.setScanDate(TODAY.toString());
        response.setEmployees(Collections.singletonList(employee));
        return response;
    }

    private DebtAlertEmployeePayload employee(DebtAlertItemPayload... items) {
        DebtAlertEmployeePayload employee = new DebtAlertEmployeePayload();
        employee.setAccountName(ACCOUNT);
        employee.setEmployeeName("Minh");
        employee.setStatus("OK");
        employee.setAlerts(Arrays.asList(items));
        return employee;
    }

    private DebtAlertItemPayload item(String supplier, String invoice, String dueDate) {
        DebtAlertItemPayload item = new DebtAlertItemPayload();
        item.setSupplier(supplier);
        item.setInvoiceNumber(invoice);
        item.setDueDate(dueDate);
        return item;
    }

    private DebtAlertTaskService serviceAt(int year, int month, int day, int hour, int minute) {
        DebtAlertProperties properties = new DebtAlertProperties();
        properties.setTaskDeadlineHour(16);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(year, month, day, hour, minute, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        return new DebtAlertTaskService(monthEventRepository, accountRepository,
                properties, clock);
    }
}
