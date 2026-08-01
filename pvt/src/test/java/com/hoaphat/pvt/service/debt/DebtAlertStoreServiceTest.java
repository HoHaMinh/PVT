package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.debt.DebtAlertSync;
import com.hoaphat.pvt.model.debt.DebtAlertSyncStatus;
import com.hoaphat.pvt.model.dto.debt.*;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.debt.IDebtAlertSyncRepository;
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
class DebtAlertStoreServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Mock private IDebtAlertSyncRepository syncRepository;
    @Mock private IAccountRepository accountRepository;

    private DebtAlertStoreService service;
    private DebtAlertProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DebtAlertProperties();
        properties.setEnabled(true);
        properties.setWebAppUrl("https://script.google.com/macros/s/test/exec");
        properties.setSharedToken("secret");
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 7, 8, 30, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new DebtAlertStoreService(syncRepository, accountRepository, properties, clock);
        lenient().when(syncRepository.findByScanDate(TODAY)).thenReturn(Optional.empty());
    }

    @Test
    void storesValidSnapshotWithoutNullSensitiveFields() {
        when(accountRepository.findByAccountName("dminhhh"))
                .thenReturn(new Account("dminhhh", "password", "Hồ Hà Minh"));
        DebtAlertApiResponse response = response("dminhhh", "2026-07-08", null);
        ArgumentCaptor<DebtAlertSync> captor = ArgumentCaptor.forClass(DebtAlertSync.class);

        service.saveResponse(response);

        verify(syncRepository).save(captor.capture());
        DebtAlertSync saved = captor.getValue();
        assertEquals(DebtAlertSyncStatus.SUCCESS, saved.getStatus());
        assertEquals(1, saved.getTotalAlerts());
        assertEquals("dminhhh", saved.getEmployees().get(0).getAccountName());
        assertEquals(LocalDate.of(2026, 7, 8),
                saved.getEmployees().get(0).getItems().get(0).getDueDate());
        assertEquals(Integer.valueOf(-1),
                saved.getEmployees().get(0).getItems().get(0).getTPlus());
        when(syncRepository.findByScanDate(TODAY)).thenReturn(Optional.of(saved));
        DebtAlertDashboard dashboard = service.getDashboard("manager", true, false);
        assertEquals("Đợt 7 Tháng 6", dashboard.getEmployees().get(0).getReportLabel());
        assertEquals("-1 (Còn 1 ngày)",
                dashboard.getEmployees().get(0).getItems().get(0).getTPlusLabel());
    }

    @Test
    void marksUnknownAccountForManagerInsteadOfThrowingNullPointer() {
        DebtAlertApiResponse response = response("not-in-pvt", "", null);
        ArgumentCaptor<DebtAlertSync> captor = ArgumentCaptor.forClass(DebtAlertSync.class);

        service.saveResponse(response);

        verify(syncRepository).save(captor.capture());
        assertEquals("ACCOUNT_NOT_FOUND",
                captor.getValue().getEmployees().get(0).getResultStatus());
        assertNull(captor.getValue().getEmployees().get(0).getItems().get(0).getDueDate());
    }

    @Test
    void rejectsSnapshotForAnotherDay() {
        DebtAlertApiResponse response = response("dminhhh", "2026-07-08", -1);
        response.setScanDate("2026-07-06");

        assertThrows(IllegalArgumentException.class, () -> service.saveResponse(response));
        verify(syncRepository, never()).save(any());
    }

    @Test
    void employeeDashboardCannotSeeAnotherEmployeesRows() {
        DebtAlertSync sync = new DebtAlertSync();
        sync.setScanDate(TODAY);
        sync.setStatus(DebtAlertSyncStatus.SUCCESS);
        sync.setLastAttemptAt(LocalDateTime.of(2026, 7, 7, 8, 30));
        sync.setTotalAlerts(1);
        DebtAlertEmployeePayload payload = response("other.user", "2026-07-08", -1)
                .getEmployees().get(0);
        when(accountRepository.findByAccountName("other.user"))
                .thenReturn(new Account("other.user", "password", "Người khác"));
        // Save once to use the same mapping rules, then return captured snapshot.
        ArgumentCaptor<DebtAlertSync> captor = ArgumentCaptor.forClass(DebtAlertSync.class);
        service.saveResponse(responseWith(payload));
        verify(syncRepository).save(captor.capture());
        when(syncRepository.findByScanDate(TODAY)).thenReturn(Optional.of(captor.getValue()));

        DebtAlertDashboard employeeView = service.getDashboard("current.user", false, false);
        DebtAlertDashboard managerView = service.getDashboard("manager", true, false);

        assertTrue(employeeView.getEmployees().isEmpty());
        assertEquals(0, employeeView.getAlertCount());
        assertEquals(1, managerView.getEmployees().size());
        assertEquals(1, managerView.getAlertCount());
    }

    private DebtAlertApiResponse response(String accountName, String dueDate, Integer tPlus) {
        DebtAlertEmployeePayload employee = new DebtAlertEmployeePayload();
        employee.setAccountName(accountName);
        employee.setEmployeeName("Nhân viên thử nghiệm");
        employee.setSpreadsheetId("spreadsheet-id");
        employee.setSpreadsheetUrl("https://docs.google.com/spreadsheets/d/spreadsheet-id/edit");
        employee.setReportBatch("Đợt 7");
        employee.setReportMonth("6");
        employee.setStatus("OK");
        DebtAlertItemPayload item = new DebtAlertItemPayload();
        item.setSection("II.");
        item.setItemOrder("1");
        item.setSupplier(null);
        item.setInvoiceNumber("HD-01");
        item.setAmountDisplay("1.000.000");
        item.setDueDate(dueDate);
        item.setTPlus(tPlus);
        employee.setAlerts(Collections.singletonList(item));
        return responseWith(employee);
    }

    private DebtAlertApiResponse responseWith(DebtAlertEmployeePayload employee) {
        DebtAlertApiResponse response = new DebtAlertApiResponse();
        response.setSuccess(true);
        response.setScanDate(TODAY.toString());
        response.setDaysOffset(7);
        response.setEmployees(Collections.singletonList(employee));
        return response;
    }
}
