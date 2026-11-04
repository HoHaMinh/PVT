package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import com.hoaphat.pvt.service.response.ResponseAttachmentStorageService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class MonthEventServiceTest {

    @Mock
    private IMonthEventRepository monthEventRepository;

    @Mock
    private IResponseEventInformationRepository responseEventInformationRepository;

    @Mock
    private IAccountRepository accountRepository;

    @Mock
    private IResponseEventInformationService responseEventInformationService;

    @Mock
    private WorkPriorityService workPriorityService;

    @Mock
    private ResponseAttachmentStorageService responseAttachmentStorageService;

    @InjectMocks
    private MonthEventService monthEventService;

    @Test
    void editMonthEventUpdatesManagedEventAndCreatesResponseLog() {
        Integer eventId = 3913;
        LocalDateTime newDeadline = LocalDateTime.of(2026, 7, 10, 16, 30);

        MonthEvent existingEvent = new MonthEvent();
        existingEvent.setMonthEventId(eventId);
        existingEvent.setMonthEventDescription("Nội dung cũ");
        existingEvent.setMonthEventDeadline(LocalDateTime.of(2026, 7, 9, 8, 0));
        existingEvent.setAll(true);

        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(eventId);
        submittedEvent.setMonthEventDescription("Nội dung mới");
        submittedEvent.setMonthEventDeadline(newDeadline);
        submittedEvent.setAccount(new Account("dtinvn", "ignored"));

        Account selectedAccount = new Account("dtinvn", "ignored", "Nhân viên thử nghiệm");
        when(monthEventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(existingEvent));
        when(accountRepository.findByAccountName("dtinvn")).thenReturn(selectedAccount);

        monthEventService.editMonthEvent(submittedEvent, "manager.test");

        assertEquals("Nội dung mới", existingEvent.getMonthEventDescription());
        assertEquals(newDeadline, existingEvent.getMonthEventDeadline());
        assertEquals(false, existingEvent.getAll());
        assertSame(selectedAccount, existingEvent.getAccount());
        verify(monthEventRepository).save(existingEvent);

        ArgumentCaptor<ResponseEventInformation> responseCaptor =
                ArgumentCaptor.forClass(ResponseEventInformation.class);
        verify(responseEventInformationService).addResponseEventInformation(responseCaptor.capture());

        ResponseEventInformation response = responseCaptor.getValue();
        assertSame(existingEvent, response.getMonthEvent());
        assertEquals("manager.test", response.getCreatedByUser());
        assertNotNull(response.getCreatedByDate());
        assertTrue(response.getEventInformationResponse().contains("16:30 10/07/2026"));
    }

    @Test
    void editMonthEventRejectsMissingDeadlineBeforeWriting() {
        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(3913);
        submittedEvent.setMonthEventDescription("Nội dung hợp lệ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> monthEventService.editMonthEvent(submittedEvent, "manager.test")
        );

        assertEquals("Deadline không được để trống.", exception.getMessage());
        verify(monthEventRepository, never()).save(submittedEvent);
        verify(responseEventInformationService, never()).addResponseEventInformation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void editMonthEventKeepsAllAssignmentWithoutRequiringAnAccountRecord() {
        Integer eventId = 3914;
        MonthEvent existingEvent = new MonthEvent();
        existingEvent.setMonthEventId(eventId);
        existingEvent.setMonthEventDescription("Việc cả phòng");
        existingEvent.setMonthEventDeadline(LocalDateTime.of(2026, 7, 7, 8, 0));
        existingEvent.setAll(true);
        existingEvent.setAccount(null);

        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(eventId);
        submittedEvent.setMonthEventDescription("Việc cả phòng đã sửa");
        submittedEvent.setMonthEventDeadline(LocalDateTime.of(2026, 7, 8, 9, 0));
        submittedEvent.setAccount(new Account("ALL", "ignored"));
        when(monthEventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(existingEvent));

        monthEventService.editMonthEvent(submittedEvent, "manager.test");

        assertEquals(true, existingEvent.getAll());
        assertEquals(null, existingEvent.getAccount());
        verify(accountRepository, never()).findByAccountName("ALL");
        verify(monthEventRepository).save(existingEvent);
    }

    @Test
    void changingAssignmentToAllClearsAnyPersonalPriority() {
        Integer eventId = 3915;
        MonthEvent existingEvent = new MonthEvent();
        existingEvent.setMonthEventId(eventId);
        existingEvent.setMonthEventDescription("Việc cá nhân");
        existingEvent.setMonthEventDeadline(LocalDateTime.of(2026, 7, 7, 8, 0));
        existingEvent.setAll(false);
        existingEvent.setAccount(new Account("employee.one", "ignored"));

        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(eventId);
        submittedEvent.setMonthEventDescription("Chuyển thành việc cả phòng");
        submittedEvent.setMonthEventDeadline(LocalDateTime.of(2026, 7, 8, 9, 0));
        submittedEvent.setAccount(new Account("ALL", "ignored"));
        when(monthEventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(existingEvent));

        monthEventService.editMonthEvent(submittedEvent, "manager.test");

        verify(workPriorityService).deleteTodayForEvent(eventId);
        assertEquals(true, existingEvent.getAll());
        assertEquals(null, existingEvent.getAccount());
    }

    @Test
    void moveToTrashKeepsEventAndAllResponses() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(100);
        when(monthEventRepository.findByIdForUpdate(100)).thenReturn(Optional.of(event));

        monthEventService.moveToTrash(100);

        assertTrue(Boolean.TRUE.equals(event.getDeleted()));
        verify(monthEventRepository).save(event);
        verify(responseEventInformationRepository, never()).deleteResponseById(100);
        verify(monthEventRepository, never()).delete(event);
    }

    @Test
    void bulkDeadlineShiftValidatesEveryTaskBeforeWriting() {
        MonthEvent personal = new MonthEvent();
        personal.setMonthEventId(201);
        personal.setAll(false);
        personal.setDeleted(false);
        personal.setMonthEventDeadline(LocalDateTime.of(2026, 7, 29, 11, 0));
        MonthEvent allTask = new MonthEvent();
        allTask.setMonthEventId(202);
        allTask.setAll(true);
        allTask.setDeleted(false);
        allTask.setMonthEventDeadline(LocalDateTime.of(2026, 7, 29, 11, 0));
        when(monthEventRepository.findByIdForUpdate(201)).thenReturn(Optional.of(personal));
        when(monthEventRepository.findByIdForUpdate(202)).thenReturn(Optional.of(allTask));

        assertThrows(IllegalArgumentException.class,
                () -> monthEventService.shiftDeadlines(Arrays.asList(201, 202),
                        DeadlineShiftOption.ONE_DAY, "manager.one"));

        verify(monthEventRepository, never()).save(org.mockito.ArgumentMatchers.any(MonthEvent.class));
        verify(responseEventInformationService, never())
                .addResponseEventInformation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bulkDeadlineShiftPreservesTaskDataAndCreatesUsualResponseForEachTask() {
        MonthEvent first = personalEvent(203, LocalDateTime.of(2026, 7, 29, 11, 0));
        MonthEvent second = personalEvent(204, LocalDateTime.of(2026, 7, 29, 16, 0));
        when(monthEventRepository.findByIdForUpdate(203)).thenReturn(Optional.of(first));
        when(monthEventRepository.findByIdForUpdate(204)).thenReturn(Optional.of(second));

        monthEventService.shiftDeadlines(Arrays.asList(203, 204),
                DeadlineShiftOption.ONE_SESSION, "manager.one");

        assertEquals(LocalDateTime.of(2026, 7, 29, 16, 0), first.getMonthEventDeadline());
        assertEquals(LocalDateTime.of(2026, 7, 30, 11, 0), second.getMonthEventDeadline());
        assertEquals("Công việc 203", first.getMonthEventDescription());
        assertEquals("employee.one", first.getAccount().getAccountName());
        verify(monthEventRepository).save(first);
        verify(monthEventRepository).save(second);
        verify(responseEventInformationService, org.mockito.Mockito.times(2))
                .addResponseEventInformation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void manualBulkDeadlineUpdatesOnlyChangedTasksAndSkipsUnchangedResponseLog() {
        LocalDateTime targetDeadline = LocalDateTime.of(2026, 8, 4, 16, 0);
        MonthEvent morningTask = personalEvent(205, LocalDateTime.of(2026, 8, 4, 11, 0));
        MonthEvent alreadyCorrectTask = personalEvent(206, targetDeadline);
        when(monthEventRepository.findByIdForUpdate(205)).thenReturn(Optional.of(morningTask));
        when(monthEventRepository.findByIdForUpdate(206)).thenReturn(Optional.of(alreadyCorrectTask));

        int changedCount = monthEventService.setDeadlines(
                Arrays.asList(205, 206), targetDeadline, "manager.one");

        assertEquals(1, changedCount);
        assertEquals(targetDeadline, morningTask.getMonthEventDeadline());
        assertEquals(targetDeadline, alreadyCorrectTask.getMonthEventDeadline());
        verify(monthEventRepository).save(morningTask);
        verify(monthEventRepository, never()).save(alreadyCorrectTask);
        verify(responseEventInformationService)
                .addResponseEventInformation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void restoreFromTrashMakesEventVisibleAndPreservesItsTypeAndResponses() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(101);
        event.setMonthEventStatus(1);
        event.setDeleted(true);
        event.setHidden(true);
        when(monthEventRepository.findByIdForUpdate(101)).thenReturn(Optional.of(event));

        monthEventService.restoreFromTrash(101);

        assertEquals(false, event.getDeleted());
        assertEquals(false, event.getHidden());
        assertEquals(1, event.getMonthEventStatus());
        verify(monthEventRepository).save(event);
        verify(responseEventInformationRepository, never()).deleteResponseById(101);
    }

    @Test
    void permanentlyDeleteRemovesResponsesBeforeDeletedEvent() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(102);
        event.setDeleted(true);
        when(monthEventRepository.findByIdForUpdate(102)).thenReturn(Optional.of(event));

        monthEventService.permanentlyDelete(102);

        InOrder deletionOrder = inOrder(responseEventInformationRepository, monthEventRepository);
        deletionOrder.verify(responseEventInformationRepository).deleteResponseById(102);
        deletionOrder.verify(monthEventRepository).delete(event);
    }

    @Test
    void permanentlyDeleteRefusesAnActiveEvent() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(103);
        event.setDeleted(false);
        when(monthEventRepository.findByIdForUpdate(103)).thenReturn(Optional.of(event));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> monthEventService.permanentlyDelete(103));

        assertEquals("Công việc không nằm trong Thư mục đã xóa.", exception.getMessage());
        verify(responseEventInformationRepository, never()).deleteResponseById(103);
        verify(monthEventRepository, never()).delete(event);
    }

    @Test
    void emptyTrashDeletesResponsesAndEventsForEveryDeletedItem() {
        MonthEvent first = new MonthEvent();
        first.setMonthEventId(104);
        first.setDeleted(true);
        MonthEvent second = new MonthEvent();
        second.setMonthEventId(105);
        second.setDeleted(true);
        when(monthEventRepository.findDeletedEventsForUpdate()).thenReturn(Arrays.asList(first, second));

        int deletedCount = monthEventService.emptyTrash();

        assertEquals(2, deletedCount);
        InOrder deletionOrder = inOrder(responseEventInformationRepository, monthEventRepository);
        deletionOrder.verify(responseEventInformationRepository).deleteResponseById(104);
        deletionOrder.verify(monthEventRepository).delete(first);
        deletionOrder.verify(responseEventInformationRepository).deleteResponseById(105);
        deletionOrder.verify(monthEventRepository).delete(second);
    }

    @Test
    void deletedTaskSearchTrimsContentAndDelegatesPaging() {
        PageRequest pageable = PageRequest.of(2, 11);
        Page<MonthEvent> expected = new PageImpl<>(Collections.singletonList(new MonthEvent()));
        when(monthEventRepository.searchDeletedEvents("báo cáo", pageable)).thenReturn(expected);

        Page<MonthEvent> actual = monthEventService.searchDeletedMonthEvents(
                pageable, "  báo cáo  ");

        assertSame(expected, actual);
        verify(monthEventRepository).searchDeletedEvents("báo cáo", pageable);
    }

    @Test
    void addMonthEventsValidatesEntireBatchBeforeWriting() {
        Account selectedAccount = new Account("dtinvn", "ignored", "Đinh Văn Tín");
        when(accountRepository.findByAccountName("dtinvn")).thenReturn(selectedAccount);

        MonthEvent valid = new MonthEvent();
        valid.setMonthEventDescription("Công việc hợp lệ");
        valid.setMonthEventDeadline(LocalDateTime.of(2026, 7, 5, 8, 0));
        valid.setMonthEventStatus(0);
        valid.setAccount(new Account("dtinvn", "ignored"));

        MonthEvent invalid = new MonthEvent();
        invalid.setMonthEventDescription("   ");
        invalid.setMonthEventDeadline(LocalDateTime.of(2026, 7, 6, 8, 0));
        invalid.setMonthEventStatus(0);
        invalid.setAccount(new Account("dtinvn", "ignored"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> monthEventService.addMonthEvents(Arrays.asList(valid, invalid), 0));

        assertTrue(error.getMessage().startsWith("Dòng 2:"));
        verify(monthEventRepository, never()).save(org.mockito.ArgumentMatchers.any(MonthEvent.class));
    }

    @Test
    void addWeekEventRejectsExtendDayAbove365() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventDescription("Công việc lặp lại");
        event.setMonthEventDeadline(LocalDateTime.of(2026, 7, 5, 8, 0));
        event.setMonthEventStatus(2);
        event.setExtendDay(366);
        event.setAccount(new Account("ALL", "ignored"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> monthEventService.addMonthEvents(Collections.singletonList(event), 2));

        assertEquals("Dòng 1: Số ngày gia hạn phải từ 1 đến 365.", error.getMessage());
        verify(monthEventRepository, never()).save(org.mockito.ArgumentMatchers.any(MonthEvent.class));
    }

    @Test
    void addMonthEventsRejectsLegacyMonthStatus() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventDescription("Việc tháng cũ");
        event.setMonthEventDeadline(LocalDateTime.of(2026, 7, 5, 8, 0));
        event.setMonthEventStatus(1);
        event.setAccount(new Account("ALL", "ignored"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> monthEventService.addMonthEvents(Collections.singletonList(event), 0));

        assertEquals("Dòng 1: Loại công việc không hợp lệ.", error.getMessage());
        verify(monthEventRepository, never()).save(org.mockito.ArgumentMatchers.any(MonthEvent.class));
    }

    @Test
    void employeeSelfAssignmentAlwaysTargetsLoggedInAccount() {
        Account employee = new Account("employee.one", "ignored", "Nhân viên Một");
        when(accountRepository.findByAccountName("employee.one")).thenReturn(employee);

        LocalDateTime deadline = LocalDateTime.of(2026, 7, 8, 16, 0);
        monthEventService.addSelfAssignedEvent(
                "employee.one", "  Việc tự giao  ", deadline);

        ArgumentCaptor<MonthEvent> captor = ArgumentCaptor.forClass(MonthEvent.class);
        verify(monthEventRepository).save(captor.capture());
        MonthEvent saved = captor.getValue();
        assertSame(employee, saved.getAccount());
        assertEquals("Việc tự giao", saved.getMonthEventDescription());
        assertEquals(deadline, saved.getMonthEventDeadline());
        assertEquals(0, saved.getMonthEventStatus());
        assertEquals(false, saved.getAll());
        assertEquals(false, saved.getHidden());
        assertEquals(false, saved.getDeleted());
    }

    private MonthEvent personalEvent(int id, LocalDateTime deadline) {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(id);
        event.setMonthEventDescription("Công việc " + id);
        event.setMonthEventDeadline(deadline);
        event.setAll(false);
        event.setDeleted(false);
        event.setAccount(new Account("employee.one", "ignored"));
        return event;
    }
}
