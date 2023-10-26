package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.event.MonthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface IMonthEventService {
    List<MonthEvent> getMonthEventList();

    Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable);

    void deleteMonthEvent(LocalDateTime now);

    void addMonthEvent(MonthEvent monthEvent);

    List<MonthEvent> getMonthEventListByTime(LocalDateTime now);

    List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String name);

    List<MonthEvent> getWeekEventList();

    void checkWeekEventDeadline (LocalDateTime now);

    void deleteWeekEvent(Integer id);

    MonthEvent findById (Integer id);
}
