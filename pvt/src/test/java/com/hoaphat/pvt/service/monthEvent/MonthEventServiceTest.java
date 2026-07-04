package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
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
}
