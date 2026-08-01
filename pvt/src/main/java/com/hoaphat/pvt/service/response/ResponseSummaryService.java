package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.*;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResponseSummaryService {
    private static final DateTimeFormatter RESPONSE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DEADLINE_ISO_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final IResponseEventInformationRepository responseRepository;
    private final IAccountRepository accountRepository;
    private final AccountService accountService;
    private final WorkPriorityService workPriorityService;
    private final ResponseAttachmentStorageService attachmentStorageService;
    private final Clock clock;

    public ResponseSummaryService(IResponseEventInformationRepository responseRepository,
                                  IAccountRepository accountRepository,
                                  AccountService accountService,
                                  WorkPriorityService workPriorityService,
                                  ResponseAttachmentStorageService attachmentStorageService,
                                  Clock clock) {
        this.responseRepository = responseRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.workPriorityService = workPriorityService;
        this.attachmentStorageService = attachmentStorageService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResponseSummaryPreview getTodayPreview(String accountName) {
        String target = requireAccount(accountName);
        ResponseSummaryPeriod period = ResponseSummaryPeriod.TODAY;
        List<ResponseEventInformation> responses = loadResponses(target, period);
        String displayName = accountService.getDisplayName(target);
        Set<Integer> taskIds = new HashSet<Integer>();
        for (ResponseEventInformation response : responses) {
            if (response != null && response.getMonthEvent() != null
                    && response.getMonthEvent().getMonthEventId() != null) {
                taskIds.add(response.getMonthEvent().getMonthEventId());
            }
        }
        return new ResponseSummaryPreview(target, displayName,
                "Tổng hợp phản hồi của " + displayName + " " + period.getDisplayLabel(),
                taskIds.size(), responses.size());
    }

    @Transactional(readOnly = true)
    public ResponseSummaryView getSummary(String accountName, ResponseSummaryPeriod requestedPeriod) {
        String target = requireAccount(accountName);
        ResponseSummaryPeriod period = requestedPeriod == null
                ? ResponseSummaryPeriod.TODAY : requestedPeriod;
        ResponseSummaryTimeRange range = period.resolve(now());
        List<ResponseEventInformation> responses = range.isEmpty()
                ? Collections.<ResponseEventInformation>emptyList()
                : responseRepository.findForEmployeeSummary(target,
                range.getStartInclusive(), range.getEndExclusive());
        Map<Integer, Integer> priorities = workPriorityService
                .getPrioritiesForDate(target, range.getPriorityDate());
        Map<Integer, ResponseChatMessageView> chatMessages = loadChatMessages(responses);

        LinkedHashMap<Integer, ResponseSummaryTaskView> taskMap =
                new LinkedHashMap<Integer, ResponseSummaryTaskView>();
        for (ResponseEventInformation response : responses) {
            if (response == null || response.getMonthEvent() == null
                    || response.getMonthEvent().getMonthEventId() == null) {
                continue;
            }
            MonthEvent event = response.getMonthEvent();
            Integer eventId = event.getMonthEventId();
            ResponseSummaryTaskView task = taskMap.get(eventId);
            if (task == null) {
                task = toTaskView(event, priorities.get(eventId));
                taskMap.put(eventId, task);
            }
            task.getMessages().add(toMessageView(response, chatMessages.get(response.getIdResponse())));
        }

        ResponseSummaryView result = new ResponseSummaryView();
        result.setAccountName(target);
        result.setEmployeeDisplayName(accountService.getDisplayName(target));
        result.setPeriod(period);
        result.setResponseCount(responses.size());
        result.setTasks(new ArrayList<ResponseSummaryTaskView>(taskMap.values()));
        result.setTaskCount(result.getTasks().size());
        return result;
    }

    @Transactional(readOnly = true)
    public Set<Integer> getSelectableEventIds(String accountName, ResponseSummaryPeriod period) {
        ResponseSummaryView summary = getSummary(accountName, period);
        Set<Integer> result = new LinkedHashSet<Integer>();
        for (ResponseSummaryTaskView task : summary.getTasks()) {
            if (task.isSelectable() && task.getMonthEventId() != null) {
                result.add(task.getMonthEventId());
            }
        }
        return result;
    }

    private List<ResponseEventInformation> loadResponses(String accountName,
                                                         ResponseSummaryPeriod period) {
        ResponseSummaryTimeRange range = period.resolve(now());
        if (range.isEmpty()) {
            return Collections.emptyList();
        }
        return responseRepository.findForEmployeeSummary(accountName,
                range.getStartInclusive(), range.getEndExclusive());
    }

    private Map<Integer, ResponseChatMessageView> loadChatMessages(
            List<ResponseEventInformation> responses) {
        Map<Integer, ResponseChatMessageView> result =
                new HashMap<Integer, ResponseChatMessageView>();
        if (responses == null || responses.isEmpty()) {
            return result;
        }
        for (ResponseChatMessageView view : attachmentStorageService.toChatMessageViews(responses)) {
            if (view != null && view.getIdResponse() != null) {
                result.put(view.getIdResponse(), view);
            }
        }
        return result;
    }

    private ResponseSummaryTaskView toTaskView(MonthEvent event, Integer priority) {
        ResponseSummaryTaskView task = new ResponseSummaryTaskView();
        task.setMonthEventId(event.getMonthEventId());
        task.setDescription(event.getMonthEventDescription() == null
                ? "Nội dung công việc cũ bị thiếu" : event.getMonthEventDescription());
        task.setDeadline(event.getMonthEventDeadline() == null
                ? "Chưa có deadline" : event.getMonthEventDeadline().format(DEADLINE_FORMAT));
        task.setDeadlineIso(event.getMonthEventDeadline() == null
                ? "" : event.getMonthEventDeadline().format(DEADLINE_ISO_FORMAT));
        boolean allTask = Boolean.TRUE.equals(event.getAll());
        task.setAllTask(allTask);
        task.setSelectable(!allTask);
        task.setDailyPriority(allTask ? null : priority);
        return task;
    }

    private ResponseSummaryMessageView toMessageView(ResponseEventInformation response,
                                                     ResponseChatMessageView chatMessage) {
        ResponseSummaryMessageView result = new ResponseSummaryMessageView();
        result.setIdResponse(response.getIdResponse());
        String content = response.getEventInformationResponse();
        result.setContent(content == null ? "" : StringEscapeUtils.unescapeHtml4(content).trim());
        result.setCreatedAt(response.getCreatedByDate() == null
                ? "Không rõ thời gian" : response.getCreatedByDate().format(RESPONSE_TIME_FORMAT));
        if (chatMessage != null) {
            result.setAttachments(chatMessage.getAttachments());
        }
        return result;
    }

    private String requireAccount(String accountName) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được nhân viên cần tổng hợp.");
        }
        String normalized = accountName.trim();
        Account account = accountRepository.findByAccountName(normalized);
        if (account == null) {
            throw new IllegalArgumentException("Nhân viên cần tổng hợp không còn tồn tại.");
        }
        return normalized;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
