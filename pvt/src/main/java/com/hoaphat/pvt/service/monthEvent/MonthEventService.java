package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.IMonthEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonthEventService implements IMonthEventService {
    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Override
    public List<MonthEvent> getMonthEventList() {
        return monthEventRepository.findAll();
    }

    @Override
    public Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable) {
        return monthEventRepository.findMonthEventsByMonthEventStatus(0,pageable);
    }

    @Override
    public void deleteMonthEvent(LocalDateTime now) {
        monthEventRepository.updateMonthEventStatus(now);
        monthEventRepository.deleteMonthEventByStatus();
    }

    @Override
    public void addMonthEvent(MonthEvent monthEvent) {
        monthEventRepository.save(monthEvent);
    }

    @Override
    public List<MonthEvent> getMonthEventListByTime(LocalDateTime now) {
//        LocalDateTime localDateTimeAfter = now.withHour(23).withMinute(59).withSecond(59).withNano(1);
        LocalDateTime localDateTimeAfter = now.plusDays(2);
        LocalDateTime localDateTimeBefore = now.minusDays(1);
        return monthEventRepository.findMonthEventsByTime(localDateTimeBefore,localDateTimeAfter);
    }

    @Override
    public List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String name) {
        LocalDateTime localDateTimeAfter = now.plusDays(2);
        LocalDateTime localDateTimeBefore = now.minusDays(1);
        return monthEventRepository.findMonthEventsByFilter(localDateTimeBefore,localDateTimeAfter,name);
    }

    @Override
    public List<MonthEvent> getWeekEventList() {
        return monthEventRepository.findWeekEvents();
    }

    @Override
    public void checkWeekEventDeadline(LocalDateTime now) {
        List<MonthEvent> weekEvents = monthEventRepository.findAllWeekEvents();
        for (MonthEvent weekEvent: weekEvents) {
            if (now.isAfter(weekEvent.getMonthEventDeadline().plusHours(5))) {
                LocalDateTime newDeadline = weekEvent.getMonthEventDeadline().plusDays(weekEvent.getExtendDay());
                monthEventRepository.updateWeekEventDeadline(newDeadline,weekEvent.getMonthEventId());
            }
        }
    }

    @Override
    public void deleteWeekEvent(Integer id) {
        monthEventRepository.deleteById(id);
    }

    @Override
    public MonthEvent findById(Integer id) {
        return monthEventRepository.findById(id).orElse(null);
    }
}
