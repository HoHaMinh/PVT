package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.model.reminder.PersonalReminderView;
import com.hoaphat.pvt.service.ISercurityScheduleService;
import com.hoaphat.pvt.service.UserTabOrderService;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import com.hoaphat.pvt.service.reminder.PersonalDailyReminderService;
import com.hoaphat.pvt.service.response.ResponseSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalReminderPrivatePageTest {
    @Mock private IMonthEventService monthEventService;
    @Mock private ISercurityScheduleService sercurityScheduleService;
    @Mock private AccountService accountService;
    @Mock private UserTabOrderService userTabOrderService;
    @Mock private WorkPriorityService workPriorityService;
    @Mock private ResponseSummaryService responseSummaryService;
    @Mock private PersonalDailyReminderService personalDailyReminderService;

    private EventController controller;

    @BeforeEach
    void setUp() {
        controller = new EventController();
        ReflectionTestUtils.setField(controller, "monthEventService", monthEventService);
        ReflectionTestUtils.setField(controller, "sercurityScheduleService", sercurityScheduleService);
        ReflectionTestUtils.setField(controller, "accountService", accountService);
        ReflectionTestUtils.setField(controller, "userTabOrderService", userTabOrderService);
        ReflectionTestUtils.setField(controller, "workPriorityService", workPriorityService);
        ReflectionTestUtils.setField(controller, "responseSummaryService", responseSummaryService);
        ReflectionTestUtils.setField(controller, "personalDailyReminderService",
                personalDailyReminderService);
        when(sercurityScheduleService.getAll()).thenReturn(Collections.emptyList());
        when(accountService.getUserDisplayList()).thenReturn(Arrays.asList(
                new UserDisplayDTO("manager", "Quản lý"),
                new UserDisplayDTO("employee.one", "Nhân viên Một")));
        when(monthEventService.getMonthEventListByFilter(any(), anyString()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void managerViewingEmployeeTabStillReceivesOnlyManagersReminder() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addUserRole("ROLE_MANAGER");
        request.getSession().setAttribute("accountName", "employee.one");
        request.getSession().setAttribute("username", "Quản lý");
        Principal principal = () -> "manager";
        PersonalReminderView managerReminder = new PersonalReminderView(true, false);
        when(personalDailyReminderService.getTodayForAccount("manager"))
                .thenReturn(managerReminder);
        when(userTabOrderService.getOrderedTabs("Quản lý"))
                .thenReturn(Collections.emptyList());

        Model model = new ExtendedModelMap();
        controller.showPrivate(model, Optional.of("employee.one"), request, principal);

        assertSame(managerReminder, model.getAttribute("personalReminder"));
        verify(personalDailyReminderService).getTodayForAccount("manager");
        verify(personalDailyReminderService, never()).getTodayForAccount("employee.one");
        verify(monthEventService).getMonthEventListByFilter(any(), eq("employee.one"));
    }

}
