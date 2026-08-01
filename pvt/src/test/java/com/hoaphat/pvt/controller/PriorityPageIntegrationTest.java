package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.DailyPriorityForm;
import com.hoaphat.pvt.model.dto.PriorityTaskRow;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.site-name=Phòng vật tư Ống thép Hòa Phát Test")
@AutoConfigureMockMvc
class PriorityPageIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private WorkPriorityService workPriorityService;
    @MockBean private AccountService accountService;
    @MockBean private NotificationService notificationService;

    private DailyPriorityForm newSelectionForm;

    @BeforeEach
    void setUp() {
        PriorityTaskRow row = new PriorityTaskRow();
        row.setMonthEventId(10);
        row.setDescription("Báo cáo mục ABC");
        row.setDeadline("16:00 08-07-2026");
        row.setAllTask(false);

        newSelectionForm = new DailyPriorityForm();
        newSelectionForm.setAccountName("employee.one");
        newSelectionForm.setAlreadySubmitted(false);
        newSelectionForm.setTasks(Collections.singletonList(row));
    }

    @Test
    void employeePriorityPageRendersTheEmployeeMenuAndSelection() throws Exception {
        when(workPriorityService.getTodayForm("employee.one")).thenReturn(newSelectionForm);

        mockMvc.perform(get("/home/employee/priorities")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("priority"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giao việc bản thân")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Báo cáo mục ABC")));
    }

    @Test
    void managerPriorityPageRendersExistingPriorityEditColumns() throws Exception {
        DailyPriorityForm editForm = newSelectionForm;
        editForm.setAlreadySubmitted(true);
        editForm.getTasks().get(0).setSelectedPriority(2);
        editForm.getTasks().get(0).setPriority(2);
        Account employee = new Account("employee.one", "ignored", "Nhân viên Một");
        when(accountService.findAll()).thenReturn(Collections.singletonList(employee));
        when(workPriorityService.getTodayForm("employee.one")).thenReturn(editForm);

        mockMvc.perform(get("/home/manager/priorities")
                        .param("accountName", "employee.one")
                        .with(user("manager.one").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("priority"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ưu tiên đã chọn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sửa ưu tiên")));
    }
}
