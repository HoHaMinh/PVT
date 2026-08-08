package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.model.dto.ResponseSummaryPeriod;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResponseSummaryActionService {
    private final ResponseSummaryService responseSummaryService;
    private final IMonthEventService monthEventService;

    public ResponseSummaryActionService(ResponseSummaryService responseSummaryService,
                                        IMonthEventService monthEventService) {
        this.responseSummaryService = responseSummaryService;
        this.monthEventService = monthEventService;
    }

    @Transactional
    public int shiftDeadlines(String accountName, ResponseSummaryPeriod period,
                              Collection<Integer> requestedIds, DeadlineShiftOption shiftOption,
                              String editedBy) {
        List<Integer> selectedIds = validateSelection(accountName, period, requestedIds);
        monthEventService.shiftDeadlines(selectedIds, shiftOption, editedBy);
        return selectedIds.size();
    }

    @Transactional
    public int setDeadlines(String accountName, ResponseSummaryPeriod period,
                            Collection<Integer> requestedIds, LocalDateTime targetDeadline,
                            String editedBy) {
        List<Integer> selectedIds = validateSelection(accountName, period, requestedIds);
        return monthEventService.setDeadlines(selectedIds, targetDeadline, editedBy);
    }

    @Transactional
    public int moveToTrash(String accountName, ResponseSummaryPeriod period,
                           Collection<Integer> requestedIds) {
        List<Integer> selectedIds = validateSelection(accountName, period, requestedIds);
        monthEventService.moveToTrash(selectedIds);
        return selectedIds.size();
    }

    private List<Integer> validateSelection(String accountName, ResponseSummaryPeriod period,
                                            Collection<Integer> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một công việc.");
        }
        LinkedHashSet<Integer> selected = new LinkedHashSet<Integer>();
        for (Integer id : requestedIds) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("Danh sách công việc không hợp lệ.");
            }
            selected.add(id);
        }

        Set<Integer> eligible = responseSummaryService.getSelectableEventIds(
                accountName, period == null ? ResponseSummaryPeriod.TODAY : period);
        if (!eligible.containsAll(selected)) {
            throw new IllegalArgumentException(
                    "Danh sách đã chọn có công việc không còn thuộc bản tổng hợp hoặc là việc Cả phòng.");
        }
        return new ArrayList<Integer>(selected);
    }
}
