package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.debt.DebtAlertDashboard;
import com.hoaphat.pvt.model.dto.debt.DebtAlertEmployeeView;
import com.hoaphat.pvt.model.dto.debt.DebtAlertItemView;
import com.hoaphat.pvt.model.dto.debt.DebtImportAlertView;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import com.hoaphat.pvt.service.debt.DebtAlertSyncCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.site-name=Phòng vật tư Ống thép Hòa Phát Test",
        "app.debt-alert.enabled=true"
})
@AutoConfigureMockMvc
class DebtAlertPageIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private DebtAlertStoreService storeService;
    @MockBean private DebtAlertSyncCoordinator syncCoordinator;

    @Test
    void rendersDebtTableForEmployee() throws Exception {
        when(syncCoordinator.isRunning()).thenReturn(false);
        when(storeService.getDashboard(eq("employee.one"), eq(false), eq(false)))
                .thenReturn(dashboard());

        mockMvc.perform(get("/home/employee/debt-alerts")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("debtAlerts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cảnh báo công nợ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhà cung cấp thử nghiệm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HD-01")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cảnh báo hồ sơ nhập khẩu cần báo kế toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NK-01")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Đã báo kế toán"))));
    }

    @Test
    void employeeCannotAcknowledgeImportAlert() throws Exception {
        mockMvc.perform(post("/home/manager/debt-alerts/import-alerts/ack")
                        .param("alertKey", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .with(user("employee.one").roles("EMPLOYEE"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verifyNoInteractions(storeService);
    }

    @Test
    void managerCanAcknowledgeImportAlert() throws Exception {
        mockMvc.perform(post("/home/manager/debt-alerts/import-alerts/ack")
                        .param("alertKey", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .with(user("manager.one").roles("MANAGER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/employee/debt-alerts"));

        org.mockito.Mockito.verify(storeService).acknowledgeImportAlert(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "manager.one", true);
    }

    private DebtAlertDashboard dashboard() {
        DebtAlertItemView item = new DebtAlertItemView();
        item.setSection("II.");
        item.setItemOrder("1");
        item.setSupplier("Nhà cung cấp thử nghiệm");
        item.setInvoiceNumber("HD-01");
        item.setAmountDisplay("1.000.000");
        item.setDueDate("08/07/2026");
        item.setTPlus(-1);
        item.setTPlusLabel("-1 (Còn 1 ngày)");
        item.setTPlusClass("text-success");

        DebtAlertEmployeeView employee = new DebtAlertEmployeeView();
        employee.setAccountName("employee.one");
        employee.setEmployeeName("Nhân viên Một");
        employee.setStatus("OK");
        employee.setItems(Collections.singletonList(item));

        DebtAlertDashboard dashboard = new DebtAlertDashboard();
        dashboard.setEnabled(true);
        dashboard.setConfigured(true);
        dashboard.setStatus("SUCCESS");
        dashboard.setScanDate("07/07/2026");
        dashboard.setLastSuccessAt("08:30 07/07/2026");
        dashboard.setAlertCount(1);
        dashboard.setEmployees(Collections.singletonList(employee));
        DebtImportAlertView importAlert = new DebtImportAlertView();
        importAlert.setAlertKey("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        importAlert.setAccountName("employee.one");
        importAlert.setEmployeeName("Nhân viên Một");
        importAlert.setSection("II.");
        importAlert.setSupplier("Nhà cung cấp NK");
        importAlert.setInvoiceNumber("NK-01");
        importAlert.setRemainingAmountDisplay("100.000.000");
        importAlert.setBillOfLadingDate("01/05/2026");
        importAlert.setCustomsDeclarationDate("15/06/2026");
        importAlert.setRegulatoryDeadline("30/07/2026");
        importAlert.setDaysRemaining(5);
        importAlert.setDeadlineLabel("Còn 5 ngày");
        importAlert.setDeadlineClass("text-warning");
        dashboard.setImportAlertCount(1);
        dashboard.setImportAlerts(Collections.singletonList(importAlert));
        return dashboard;
    }
}
