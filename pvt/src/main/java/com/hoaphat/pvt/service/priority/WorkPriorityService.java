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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkPriorityService {
    private static final int MAX_PRIORITIZED_TASKS = 7;
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");

    private final IAccountRepository accountRepository;
    private final IMonthEventRepository monthEventRepository;
    private final IDailyPrioritySelectionRepository selectionRepository;
    private final ITaskPriorityRepository taskPriorityRepository;
    private final Clock clock;

    public WorkPriorityService(IAccountRepository accountRepository,
                               IMonthEventRepository monthEventRepository,
                               IDailyPrioritySelectionRepository selectionRepository,
                               ITaskPriorityRepository taskPriorityRepository,
                               Clock clock) {
        this.accountRepository = accountRepository;
        this.monthEventRepository = monthEventRepository;
        this.selectionRepository = selectionRepository;
        this.taskPriorityRepository = taskPriorityRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DailyPriorityForm getTodayForm(String accountName) {
        String normalizedAccountName = requireAccountName(accountName);
        requireAccount(normalizedAccountName);

        Optional<DailyPrioritySelection> selection = selectionRepository
                .findByAccount_AccountNameAndPriorityDate(normalizedAccountName, today());
        Map<Integer, Integer> selectedValues = selection
                .map(this::loadPriorityMap)
                .orElseGet(Collections::emptyMap);

        DailyPriorityForm form = new DailyPriorityForm();
        form.setAccountName(normalizedAccountName);
        form.setAlreadySubmitted(selection.isPresent());

        List<PriorityTaskRow> rows = getCandidateEvents(normalizedAccountName).stream()
                .map(event -> toRow(event, selectedValues.get(event.getMonthEventId())))
                .collect(Collectors.toList());
        form.setTasks(rows);
        return form;
    }

    @Transactional
    public void submitEmployeePriorities(String accountName, List<PriorityTaskRow> rows) {
        savePriorities(accountName, accountName, rows, false);
    }

    @Transactional
    public void saveManagerPriorities(String managerAccountName, String targetAccountName,
                                      List<PriorityTaskRow> rows) {
        savePriorities(managerAccountName, targetAccountName, rows, true);
    }

    @Transactional(readOnly = true)
    public void applyTodayPriorities(List<MonthEvent> events, String accountName) {
        if (events == null || events.isEmpty() || accountName == null || accountName.trim().isEmpty()) {
            return;
        }
        Optional<DailyPrioritySelection> selection = selectionRepository
                .findByAccount_AccountNameAndPriorityDate(accountName.trim(), today());
        Map<Integer, Integer> priorities = selection
                .map(this::loadPriorityMap)
                .orElseGet(Collections::emptyMap);
        for (MonthEvent event : events) {
            if (event != null) {
                event.setDailyPriority(Boolean.TRUE.equals(event.getAll())
                        ? null : priorities.get(event.getMonthEventId()));
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<Integer, Integer> getPrioritiesForDate(String accountName, LocalDate priorityDate) {
        if (accountName == null || accountName.trim().isEmpty() || priorityDate == null) {
            return Collections.emptyMap();
        }
        Optional<DailyPrioritySelection> selection = selectionRepository
                .findByAccount_AccountNameAndPriorityDate(accountName.trim(), priorityDate);
        if (!selection.isPresent()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(loadPriorityMap(selection.get())));
    }

    @Transactional
    public void deleteForEvent(Integer monthEventId) {
        if (monthEventId != null) {
            taskPriorityRepository.deleteByMonthEvent_MonthEventId(monthEventId);
        }
    }

    @Transactional
    public void deleteTodayForEvent(Integer monthEventId) {
        if (monthEventId != null) {
            taskPriorityRepository.deleteByMonthEvent_MonthEventIdAndSelection_PriorityDate(
                    monthEventId, today());
        }
    }

    @Transactional
    public void deleteForAccount(String accountName) {
        if (accountName == null || accountName.trim().isEmpty()) {
            return;
        }
        String normalized = accountName.trim();
        taskPriorityRepository.deleteBySelection_Account_AccountName(normalized);
        selectionRepository.deleteByAccount_AccountName(normalized);
    }

    private void savePriorities(String submittedBy, String targetAccountName,
                                List<PriorityTaskRow> rows, boolean managerCanEdit) {
        String actor = requireAccountName(submittedBy);
        String target = requireAccountName(targetAccountName);
        Account targetAccount = requireAccount(target);
        LocalDate date = today();

        Optional<DailyPrioritySelection> existing = selectionRepository
                .findByAccount_AccountNameAndPriorityDate(target, date);
        if (!managerCanEdit && existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Bạn đã chọn ưu tiên hôm nay. Vui lòng chờ sang ngày mới để chọn lại.");
        }

        Map<Integer, MonthEvent> candidates = getCandidateEvents(target).stream()
                .collect(Collectors.toMap(MonthEvent::getMonthEventId, Function.identity()));
        Map<Integer, Integer> normalizedChoices = validateChoices(rows, candidates);

        DailyPrioritySelection selection = existing.orElseGet(DailyPrioritySelection::new);
        selection.setAccount(targetAccount);
        selection.setPriorityDate(date);
        selection.setSubmittedAt(now());
        selection.setSubmittedBy(actor);
        try {
            selection = selectionRepository.saveAndFlush(selection);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "Ưu tiên trong ngày vừa được lưu ở một thao tác khác. Vui lòng tải lại trang.");
        }

        taskPriorityRepository.deleteBySelection_SelectionId(selection.getSelectionId());
        taskPriorityRepository.flush();

        List<TaskPriority> priorities = new ArrayList<>();
        for (Map.Entry<Integer, Integer> choice : normalizedChoices.entrySet()) {
            TaskPriority priority = new TaskPriority();
            priority.setSelection(selection);
            priority.setMonthEvent(candidates.get(choice.getKey()));
            priority.setPriorityValue(choice.getValue());
            priorities.add(priority);
        }
        taskPriorityRepository.saveAll(priorities);
    }

    private Map<Integer, Integer> validateChoices(List<PriorityTaskRow> rows,
                                                  Map<Integer, MonthEvent> candidates) {
        Map<Integer, Integer> choices = new LinkedHashMap<>();
        Set<Integer> submittedEventIds = new HashSet<>();
        if (rows == null) {
            return choices;
        }

        for (PriorityTaskRow row : rows) {
            if (row == null || row.getMonthEventId() == null) {
                if (row != null && row.getPriority() != null) {
                    throw new IllegalArgumentException("Công việc được chọn ưu tiên không hợp lệ.");
                }
                continue;
            }
            if (!submittedEventIds.add(row.getMonthEventId())) {
                throw new IllegalArgumentException("Danh sách công việc bị trùng. Vui lòng tải lại trang.");
            }
            Integer value = row.getPriority();
            if (value == null) {
                continue;
            }
            if (value < 1 || value > 7) {
                throw new IllegalArgumentException("Mức ưu tiên chỉ được từ 1 đến 7.");
            }
            MonthEvent event = candidates.get(row.getMonthEventId());
            if (event == null) {
                throw new IllegalArgumentException(
                        "Công việc được chọn không còn nằm trong danh sách hiện tại.");
            }
            if (Boolean.TRUE.equals(event.getAll())) {
                throw new IllegalArgumentException(
                        "Công việc Cả phòng không được phép chọn ưu tiên.");
            }
            choices.put(row.getMonthEventId(), value);
        }

        if (choices.size() > MAX_PRIORITIZED_TASKS) {
            throw new IllegalArgumentException("Mỗi ngày chỉ được ưu tiên tối đa 7 công việc.");
        }
        return choices;
    }

    private List<MonthEvent> getCandidateEvents(String accountName) {
        LocalDateTime current = now();
        LocalDateTime endOfTomorrow = current.toLocalDate()
                .plusDays(2)
                .atStartOfDay();
        return monthEventRepository.findByAccountFilter(
                current.minusDays(730), endOfTomorrow, accountName);
    }

    private Map<Integer, Integer> loadPriorityMap(DailyPrioritySelection selection) {
        if (selection == null || selection.getSelectionId() == null) {
            return Collections.emptyMap();
        }
        return taskPriorityRepository.findBySelection_SelectionId(selection.getSelectionId()).stream()
                .filter(item -> item.getMonthEvent() != null
                        && item.getMonthEvent().getMonthEventId() != null)
                .collect(Collectors.toMap(
                        item -> item.getMonthEvent().getMonthEventId(),
                        TaskPriority::getPriorityValue,
                        (first, ignored) -> first));
    }

    private PriorityTaskRow toRow(MonthEvent event, Integer selectedPriority) {
        PriorityTaskRow row = new PriorityTaskRow();
        row.setMonthEventId(event.getMonthEventId());
        row.setDescription(event.getMonthEventDescription());
        row.setDeadline(event.getMonthEventDeadline() == null
                ? "Chưa có deadline" : event.getMonthEventDeadline().format(DEADLINE_FORMAT));
        row.setAllTask(Boolean.TRUE.equals(event.getAll()));
        row.setSelectedPriority(selectedPriority);
        row.setPriority(selectedPriority);
        return row;
    }

    private Account requireAccount(String accountName) {
        Account account = accountRepository.findByAccountName(accountName);
        if (account == null) {
            throw new IllegalArgumentException("Tài khoản cần chọn ưu tiên không còn tồn tại.");
        }
        return account;
    }

    private String requireAccountName(String accountName) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được tài khoản.");
        }
        return accountName.trim();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
