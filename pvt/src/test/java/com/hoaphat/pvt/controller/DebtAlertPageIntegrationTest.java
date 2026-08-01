package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.debt.DebtAlertDashboard;
import com.hoaphat.pvt.model.dto.debt.DebtAlertEmployeeView;
import com.hoaphat.pvt.model.dto.debt.DebtAlertItemView;
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HD-01")));
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
        return dashboard;
    }
}
