package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface IMonthEventService {
    List<MonthEvent> getMonthEventList();

    Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable);

    void addMonthEvent(MonthEvent monthEvent, boolean flag);

    void addMonthEvents(List<MonthEvent> monthEvents, int expectedStatus);

    void addSelfAssignedEvent(String accountName, String description, LocalDateTime deadline);

    void editMonthEvent(MonthEvent monthEvent, String editedBy);

    List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String accountName);

    List<MonthEvent> getWeekEventList();

    void checkWeekEventDeadline (LocalDateTime now);

    Page<MonthEvent> getDeletedMonthEventListWithPaging(Pageable pageable);

    Page<MonthEvent> searchDeletedMonthEvents(Pageable pageable, String search);

    void moveToTrash(Integer id);

    void moveToTrash(List<Integer> ids);

    void shiftDeadlines(List<Integer> ids, DeadlineShiftOption shiftOption, String editedBy);

    int setDeadlines(List<Integer> ids, LocalDateTime targetDeadline, String editedBy);

    void restoreFromTrash(Integer id);

    void permanentlyDelete(Integer id);

    int emptyTrash();

    MonthEvent findById (Integer id);
}
