package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.event.MonthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface IMonthEventService {
    List<MonthEvent> getMonthEventList();

    Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable);

    void addMonthEvent(MonthEvent monthEvent, boolean flag);

    void editMonthEvent(MonthEvent monthEvent, String editedBy);

    List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String accountName);

    List<MonthEvent> getWeekEventList();

    void checkWeekEventDeadline (LocalDateTime now);

    Page<MonthEvent> getDeletedMonthEventListWithPaging(Pageable pageable);

    void moveToTrash(Integer id);

    void restoreFromTrash(Integer id);

    void permanentlyDelete(Integer id);

    int emptyTrash();

    MonthEvent findById (Integer id);
}
