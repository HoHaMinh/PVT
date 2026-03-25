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

    List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String accountName);

    List<MonthEvent> getWeekEventList();

    void checkWeekEventDeadline (LocalDateTime now);

    void deleteWeekEvent(Integer id);

    MonthEvent findById (Integer id);
}
