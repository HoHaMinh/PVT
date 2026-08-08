package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.model.dto.ResponseSummaryPeriod;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseSummaryActionServiceTest {
    @Mock private ResponseSummaryService responseSummaryService;
    @Mock private IMonthEventService monthEventService;
    @InjectMocks private ResponseSummaryActionService service;

    @Test
    void validSelectionIsDeduplicatedAndShiftedOnce() {
        when(responseSummaryService.getSelectableEventIds(
                "employee.one", ResponseSummaryPeriod.TODAY))
                .thenReturn(new LinkedHashSet<Integer>(Arrays.asList(10, 11)));

        int count = service.shiftDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Arrays.asList(10, 10, 11), DeadlineShiftOption.ONE_DAY, "manager.one");

        assertEquals(2, count);
        verify(monthEventService).shiftDeadlines(
                Arrays.asList(10, 11), DeadlineShiftOption.ONE_DAY, "manager.one");
    }

    @Test
    void manualDeadlineSelectionIsDeduplicatedAndDelegatedOnce() {
        LocalDateTime targetDeadline = LocalDateTime.of(2026, 8, 4, 16, 0);
        when(responseSummaryService.getSelectableEventIds(
                "employee.one", ResponseSummaryPeriod.TODAY))
                .thenReturn(new LinkedHashSet<Integer>(Arrays.asList(10, 11)));
        when(monthEventService.setDeadlines(
                Arrays.asList(10, 11), targetDeadline, "manager.one"))
                .thenReturn(2);

        int count = service.setDeadlines("employee.one", ResponseSummaryPeriod.TODAY,
                Arrays.asList(10, 10, 11), targetDeadline, "manager.one");

        assertEquals(2, count);
        verify(monthEventService).setDeadlines(
                Arrays.asList(10, 11), targetDeadline, "manager.one");
    }

    @Test
    void staleOrAllDepartmentSelectionIsRejectedBeforeAnyWrite() {
        when(responseSummaryService.getSelectableEventIds(
                "employee.one", ResponseSummaryPeriod.YESTERDAY))
                .thenReturn(new LinkedHashSet<Integer>(Arrays.asList(10)));

        assertThrows(IllegalArgumentException.class,
                () -> service.moveToTrash("employee.one", ResponseSummaryPeriod.YESTERDAY,
                        Arrays.asList(10, 99)));

        verify(monthEventService, never()).moveToTrash(Arrays.asList(10, 99));
    }
}
