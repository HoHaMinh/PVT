package com.hoaphat.pvt.service.priority;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.DailyPriorityForm;
import com.hoaphat.pvt.model.dto.PriorityTaskRow;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.priority.DailyPrioritySelection;
import com.hoaphat.pvt.model.priority.TaskPriority;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.priority.IDailyPrioritySelectionRepository;
import com.hoaphat.pvt.repository.priority.ITaskPriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkPriorityServiceTest {
    private static final String EMPLOYEE = "employee.one";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Mock private IAccountRepository accountRepository;
    @Mock private IMonthEventRepository monthEventRepository;
    @Mock private IDailyPrioritySelectionRepository selectionRepository;
    @Mock private ITaskPriorityRepository taskPriorityRepository;

    private WorkPriorityService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 7, 10, 0, 0, 0,
                        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new WorkPriorityService(accountRepository, monthEventRepository,
                selectionRepository, taskPriorityRepository, clock);
    }

    @Test
    void employeeCanPrioritizeUpToSevenTasksAndDuplicateLevelsAreAllowed() {
        Account employee = account();
        MonthEvent first = event(1, false);
        MonthEvent second = event(2, false);
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(employee);
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.empty());
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(Arrays.asList(first, second));
        when(selectionRepository.saveAndFlush(any(DailyPrioritySelection.class)))
                .thenAnswer(invocation -> {
                    DailyPrioritySelection selection = invocation.getArgument(0);
                    selection.setSelectionId(10);
                    return selection;
                });

        service.submitEmployeePriorities(EMPLOYEE,
                Arrays.asList(choice(1, 1), choice(2, 1)));

        ArgumentCaptor<List<TaskPriority>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskPriorityRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(1, captor.getValue().get(0).getPriorityValue());
        assertEquals(1, captor.getValue().get(1).getPriorityValue());
    }

    @Test
    void employeeCannotSubmitTwiceOnSameDay() {
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.of(selection(99)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.submitEmployeePriorities(EMPLOYEE,
                        Collections.singletonList(choice(1, 1))));

        assertTrue(error.getMessage().contains("đã chọn ưu tiên hôm nay"));
        verify(taskPriorityRepository, never()).saveAll(anyList());
    }

    @Test
    void moreThanSevenPrioritizedTasksIsRejectedEvenWhenLevelsRepeat() {
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.empty());
        List<MonthEvent> events = new ArrayList<>();
        List<PriorityTaskRow> rows = new ArrayList<>();
        for (int id = 1; id <= 8; id++) {
            events.add(event(id, false));
            rows.add(choice(id, 1));
        }
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(events);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.submitEmployeePriorities(EMPLOYEE, rows));

        assertEquals("Mỗi ngày chỉ được ưu tiên tối đa 7 công việc.", error.getMessage());
        verify(selectionRepository, never()).saveAndFlush(any());
    }

    @Test
    void allDepartmentTaskCannotBePrioritized() {
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.empty());
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(Collections.singletonList(event(1, true)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.submitEmployeePriorities(EMPLOYEE,
                        Collections.singletonList(choice(1, 2))));

        assertEquals("Công việc Cả phòng không được phép chọn ưu tiên.", error.getMessage());
    }

    @Test
    void managerCanReplaceExistingPriorities() {
        DailyPrioritySelection existing = selection(20);
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.of(existing));
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(Collections.singletonList(event(5, false)));
        when(selectionRepository.saveAndFlush(existing)).thenReturn(existing);

        service.saveManagerPriorities("manager.one", EMPLOYEE,
                Collections.singletonList(choice(5, 7)));

        verify(taskPriorityRepository).deleteBySelection_SelectionId(20);
        ArgumentCaptor<List<TaskPriority>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskPriorityRepository).saveAll(captor.capture());
        assertEquals(7, captor.getValue().get(0).getPriorityValue());
        assertEquals("manager.one", existing.getSubmittedBy());
    }

    @Test
    void currentPriorityIsDecoratedOnlyOnPersonalTasks() {
        MonthEvent personal = event(8, false);
        MonthEvent allTask = event(9, true);
        DailyPrioritySelection selection = selection(30);
        TaskPriority saved = new TaskPriority();
        saved.setSelection(selection);
        saved.setMonthEvent(personal);
        saved.setPriorityValue(3);
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.of(selection));
        when(taskPriorityRepository.findBySelection_SelectionId(30))
                .thenReturn(Collections.singletonList(saved));

        service.applyTodayPriorities(Arrays.asList(personal, allTask), EMPLOYEE);

        assertEquals(3, personal.getDailyPriority());
        assertNull(allTask.getDailyPriority());
    }

    @Test
    void historicalPriorityIsLoadedByRequestedDate() {
        LocalDate yesterday = TODAY.minusDays(1);
        MonthEvent personal = event(8, false);
        DailyPrioritySelection historicalSelection = selection(31);
        historicalSelection.setPriorityDate(yesterday);
        TaskPriority saved = new TaskPriority();
        saved.setSelection(historicalSelection);
        saved.setMonthEvent(personal);
        saved.setPriorityValue(2);
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, yesterday))
                .thenReturn(Optional.of(historicalSelection));
        when(taskPriorityRepository.findBySelection_SelectionId(31))
                .thenReturn(Collections.singletonList(saved));

        Map<Integer, Integer> priorities =
                service.getPrioritiesForDate(EMPLOYEE, yesterday);

        assertEquals(2, priorities.get(8));
        verify(selectionRepository).findByAccount_AccountNameAndPriorityDate(EMPLOYEE, yesterday);
    }

    @Test
    void changingAssignmentDeletesOnlyTodayPriorityAndPreservesHistory() {
        service.deleteTodayForEvent(88);

        verify(taskPriorityRepository)
                .deleteByMonthEvent_MonthEventIdAndSelection_PriorityDate(88, TODAY);
        verify(taskPriorityRepository, never()).deleteByMonthEvent_MonthEventId(88);
    }

    @Test
    void aNewDateStartsWithoutPreviousSubmission() {
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.empty());
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(Collections.emptyList());

        DailyPriorityForm form = service.getTodayForm(EMPLOYEE);

        assertFalse(form.isAlreadySubmitted());
        assertTrue(form.getTasks().isEmpty());
    }

    @Test
    void priorityListIncludesDeadlinesOnlyThroughTheEndOfTomorrow() {
        when(accountRepository.findByAccountName(EMPLOYEE)).thenReturn(account());
        when(selectionRepository.findByAccount_AccountNameAndPriorityDate(EMPLOYEE, TODAY))
                .thenReturn(Optional.empty());
        when(monthEventRepository.findByAccountFilter(any(), any(), eq(EMPLOYEE)))
                .thenReturn(Collections.emptyList());

        service.getTodayForm(EMPLOYEE);

        ArgumentCaptor<LocalDateTime> before = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> after = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(monthEventRepository).findByAccountFilter(before.capture(), after.capture(), eq(EMPLOYEE));
        assertEquals(LocalDateTime.of(2026, 7, 9, 0, 0), after.getValue());
        assertEquals(LocalDateTime.of(2024, 7, 7, 10, 0), before.getValue());
    }

    private Account account() {
        return new Account(EMPLOYEE, "ignored", "Nhân viên Một");
    }

    private DailyPrioritySelection selection(int id) {
        DailyPrioritySelection selection = new DailyPrioritySelection();
        selection.setSelectionId(id);
        selection.setAccount(account());
        selection.setPriorityDate(TODAY);
        return selection;
    }

    private MonthEvent event(int id, boolean all) {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(id);
        event.setMonthEventDescription("Công việc " + id);
        event.setMonthEventDeadline(LocalDateTime.of(2026, 7, 8, 8, 0));
        event.setAll(all);
        event.setHidden(false);
        event.setDeleted(false);
        if (!all) {
            event.setAccount(account());
        }
        return event;
    }

    private PriorityTaskRow choice(int eventId, Integer priority) {
        PriorityTaskRow row = new PriorityTaskRow();
        row.setMonthEventId(eventId);
        row.setPriority(priority);
        return row;
    }
}
