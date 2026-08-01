package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.*;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseSummaryServiceTest {
    private static final String EMPLOYEE = "ngant";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 30);

    @Mock private IResponseEventInformationRepository responseRepository;
    @Mock private IAccountRepository accountRepository;
    @Mock private AccountService accountService;
    @Mock private WorkPriorityService workPriorityService;
    @Mock private ResponseAttachmentStorageService attachmentStorageService;

    private ResponseSummaryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 30, 13, 0, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ResponseSummaryService(responseRepository, accountRepository,
                accountService, workPriorityService, attachmentStorageService, clock);
        when(accountRepository.findByAccountName(EMPLOYEE))
                .thenReturn(new Account(EMPLOYEE, "ignored", "Nguyễn Thị Nga"));
        when(accountService.getDisplayName(EMPLOYEE)).thenReturn("Nga");
    }

    @Test
    void groupsResponsesByTaskAndDecoratesHistoricalPriorities() {
        MonthEvent personal = event(10, "Dòng việc A", false);
        MonthEvent allTask = event(20, "Dòng việc B", true);
        ResponseEventInformation first = response(1, personal, "ABC", 9, 0);
        ResponseEventInformation second = response(2, personal, "XYZ", 10, 0);
        ResponseEventInformation attachmentOnly = response(3, allTask, "", 11, 0);
        when(responseRepository.findForEmployeeSummary(eq(EMPLOYEE), any(), any()))
                .thenReturn(Arrays.asList(first, second, attachmentOnly));
        when(workPriorityService.getPrioritiesForDate(EMPLOYEE, TODAY))
                .thenReturn(new LinkedHashMap<Integer, Integer>() {{
                    put(10, 2);
                    put(20, 1);
                }});

        ResponseChatMessageView firstView = new ResponseChatMessageView(first);
        ResponseChatMessageView secondView = new ResponseChatMessageView(second);
        ResponseChatMessageView attachmentView = new ResponseChatMessageView(attachmentOnly);
        ResponseAttachmentView attachment = new ResponseAttachmentView();
        attachment.setIdAttachment(99);
        attachment.setOriginalFileName("bao-cao.pdf");
        attachmentView.setAttachments(Collections.singletonList(attachment));
        when(attachmentStorageService.toChatMessageViews(anyList()))
                .thenReturn(Arrays.asList(firstView, secondView, attachmentView));

        ResponseSummaryView summary = service.getSummary(EMPLOYEE, ResponseSummaryPeriod.TODAY);

        assertEquals("Nga", summary.getEmployeeDisplayName());
        assertEquals(2, summary.getTaskCount());
        assertEquals(3, summary.getResponseCount());
        assertEquals(2, summary.getTasks().get(0).getDailyPriority());
        assertEquals("2026-07-31T16:00", summary.getTasks().get(0).getDeadlineIso());
        assertTrue(summary.getTasks().get(0).isSelectable());
        assertEquals(2, summary.getTasks().get(0).getMessages().size());
        assertEquals("09:00 30/07", summary.getTasks().get(0).getMessages().get(0).getCreatedAt());
        assertNull(summary.getTasks().get(1).getDailyPriority());
        assertFalse(summary.getTasks().get(1).isSelectable());
        assertEquals("bao-cao.pdf", summary.getTasks().get(1).getMessages()
                .get(0).getAttachments().get(0).getOriginalFileName());
    }

    @Test
    void previewCountsTodayWithoutLoadingAttachments() {
        MonthEvent event = event(10, "Dòng việc A", false);
        when(responseRepository.findForEmployeeSummary(eq(EMPLOYEE), any(), any()))
                .thenReturn(Arrays.asList(
                        response(1, event, "ABC", 9, 0),
                        response(2, event, "XYZ", 10, 0)));

        ResponseSummaryPreview preview = service.getTodayPreview(EMPLOYEE);

        assertEquals("Tổng hợp phản hồi của Nga ngày hôm nay", preview.getTitle());
        assertEquals(1, preview.getTaskCount());
        assertEquals(2, preview.getResponseCount());
        verifyNoInteractions(attachmentStorageService);
    }

    @Test
    void afternoonBeforeNoonReturnsEmptyWithoutQueryingDatabase() {
        Clock morningClock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 30, 11, 0, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        ResponseSummaryService morningService = new ResponseSummaryService(
                responseRepository, accountRepository, accountService,
                workPriorityService, attachmentStorageService, morningClock);

        ResponseSummaryView summary =
                morningService.getSummary(EMPLOYEE, ResponseSummaryPeriod.AFTERNOON);

        assertTrue(summary.getTasks().isEmpty());
        assertEquals(0, summary.getResponseCount());
        verify(responseRepository, never()).findForEmployeeSummary(anyString(), any(), any());
    }

    private MonthEvent event(int id, String description, boolean all) {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(id);
        event.setMonthEventDescription(description);
        event.setMonthEventDeadline(LocalDateTime.of(2026, 7, 31, 16, 0));
        event.setAll(all);
        event.setHidden(false);
        event.setDeleted(false);
        return event;
    }

    private ResponseEventInformation response(int id, MonthEvent event, String content,
                                              int hour, int minute) {
        return new ResponseEventInformation(id, content, event, EMPLOYEE,
                LocalDateTime.of(2026, 7, 30, hour, minute));
    }
}
