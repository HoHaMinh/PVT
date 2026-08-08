package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.ResponseSummaryMessageView;
import com.hoaphat.pvt.model.dto.ResponseSummaryPeriod;
import com.hoaphat.pvt.model.dto.ResponseSummaryTaskView;
import com.hoaphat.pvt.model.dto.ResponseSummaryView;
import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.response.ResponseSummaryActionService;
import com.hoaphat.pvt.service.response.ResponseSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.site-name=Phòng vật tư Ống thép Hòa Phát Test")
@AutoConfigureMockMvc
class ResponseSummaryPageIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ResponseSummaryService responseSummaryService;
    @MockBean private ResponseSummaryActionService actionService;
    @MockBean private NotificationService notificationService;

    @Test
    void employeeIsForcedToOwnAccountAndDoesNotSeeManagerBulkActions() throws Exception {
        when(responseSummaryService.getSummary("employee.one", ResponseSummaryPeriod.TODAY))
                .thenReturn(summary("employee.one"));

        mockMvc.perform(get("/home/employee/response-summary")
                        .param("accountName", "employee.two")
                        .param("period", "TODAY")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("responseSummary"))
                .andExpect(model().attribute("isManager", false))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trở lại tab cũ")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"managerBulkBar\""))));

        verify(responseSummaryService).getSummary("employee.one", ResponseSummaryPeriod.TODAY);
    }

    @Test
    void managerCanOpenEmployeeSummaryAndSeesBulkActions() throws Exception {
        when(responseSummaryService.getSummary("employee.two", ResponseSummaryPeriod.YESTERDAY))
                .thenReturn(summary("employee.two"));

        mockMvc.perform(get("/home/employee/response-summary")
                        .param("accountName", "employee.two")
                        .param("period", "YESTERDAY")
                        .with(user("manager.one").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("responseSummary"))
                .andExpect(model().attribute("isManager", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"managerBulkBar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("summary-task-checkbox")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đổi deadline nhiều mục")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý xóa nhiều mục")));

        verify(responseSummaryService).getSummary("employee.two", ResponseSummaryPeriod.YESTERDAY);
    }

    @Test
    void managerDoesNotSeeBulkActionsWhenSummaryHasNoTasks() throws Exception {
        ResponseSummaryView emptySummary = new ResponseSummaryView();
        emptySummary.setAccountName("employee.two");
        emptySummary.setEmployeeDisplayName("Employee Two");
        emptySummary.setPeriod(ResponseSummaryPeriod.TODAY);
        emptySummary.setTaskCount(0);
        emptySummary.setResponseCount(0);
        emptySummary.setTasks(Collections.emptyList());
        when(responseSummaryService.getSummary("employee.two", ResponseSummaryPeriod.TODAY))
                .thenReturn(emptySummary);

        mockMvc.perform(get("/home/employee/response-summary")
                        .param("accountName", "employee.two")
                        .param("period", "TODAY")
                        .with(user("manager.one").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"managerBulkBar\""))));
    }

    @Test
    void employeeCannotCallManagerBulkEndpoint() throws Exception {
        mockMvc.perform(post("/home/manager/response-summary/delete")
                        .param("accountName", "employee.one")
                        .param("period", "TODAY")
                        .param("eventIds", "100")
                        .with(csrf())
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerBulkDeadlineCallsTransactionServiceAndSendsOneRefresh() throws Exception {
        when(actionService.shiftDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Collections.singletonList(100), DeadlineShiftOption.ONE_SESSION, "manager.one"))
                .thenReturn(1);

        mockMvc.perform(post("/home/manager/response-summary/shift-deadlines")
                        .param("accountName", "employee.one")
                        .param("period", "TODAY")
                        .param("eventIds", "100")
                        .param("shiftOption", "ONE_SESSION")
                        .with(csrf())
                        .with(user("manager.one").roles("MANAGER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/home/employee/response-summary?accountName=employee.one&period=TODAY"));

        verify(actionService).shiftDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Collections.singletonList(100), DeadlineShiftOption.ONE_SESSION, "manager.one");
        verify(notificationService).sendRefreshSignal();
    }

    @Test
    void managerBulkManualDeadlineCallsTransactionServiceAndSendsOneRefresh() throws Exception {
        LocalDateTime targetDeadline = LocalDateTime.of(2026, 8, 4, 16, 0);
        when(actionService.setDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Collections.singletonList(100), targetDeadline, "manager.one"))
                .thenReturn(1);

        mockMvc.perform(post("/home/manager/response-summary/shift-deadlines")
                        .param("accountName", "employee.one")
                        .param("period", "TODAY")
                        .param("eventIds", "100")
                        .param("deadlineMode", "MANUAL_DEADLINE")
                        .param("manualDeadline", "2026-08-04T16:00")
                        .with(csrf())
                        .with(user("manager.one").roles("MANAGER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/home/employee/response-summary?accountName=employee.one&period=TODAY"));

        verify(actionService).setDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Collections.singletonList(100), targetDeadline, "manager.one");
        verify(notificationService).sendRefreshSignal();
    }

    private ResponseSummaryView summary(String accountName) {
        ResponseSummaryMessageView message = new ResponseSummaryMessageView();
        message.setIdResponse(10);
        message.setCreatedAt("09:00 30/07");
        message.setContent("Đã hoàn thành bước một");

        ResponseSummaryTaskView task = new ResponseSummaryTaskView();
        task.setMonthEventId(100);
        task.setDescription("Báo cáo tiến độ");
        task.setDeadline("16:00 30/07/2026");
        task.setSelectable(true);
        task.setMessages(Collections.singletonList(message));

        ResponseSummaryView summary = new ResponseSummaryView();
        summary.setAccountName(accountName);
        summary.setEmployeeDisplayName("Nguyễn Văn A");
        summary.setPeriod(ResponseSummaryPeriod.TODAY);
        summary.setTaskCount(1);
        summary.setResponseCount(1);
        summary.setTasks(Collections.singletonList(task));
        return summary;
    }
}
