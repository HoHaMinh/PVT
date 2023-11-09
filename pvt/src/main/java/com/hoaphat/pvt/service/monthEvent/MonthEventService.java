package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
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

    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;

    @Override
    public List<MonthEvent> getMonthEventList() {
        return monthEventRepository.findAll();
    }

    //    *Trang private
    @Override
    public List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String name) {
        LocalDateTime localDateTimeAfter = now.plusDays(2);
        LocalDateTime localDateTimeBefore = now.minusDays(1);
        return monthEventRepository.findMonthEventsByFilter(localDateTimeBefore, localDateTimeAfter, name);
    }

    //* Trang task
    @Override
    public Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable) {
        return monthEventRepository.findMonthEventsByMonthEventStatus(0, pageable);
    }

    @Override
    public void addMonthEvent(MonthEvent monthEvent) {
        monthEventRepository.save(monthEvent);
    }

    @Override
    public MonthEvent findById(Integer id) {
        return monthEventRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteWeekEvent(Integer id) {
        responseEventInformationRepository.deleteResponseById(id);
        monthEventRepository.deleteById(id);
    }

    //    *Trang weekly task
    @Override
    public List<MonthEvent> getWeekEventList() {
        return monthEventRepository.findWeekEvents();
    }

    @Override
    public void checkWeekEventDeadline(LocalDateTime now) {
        List<MonthEvent> weekEvents = monthEventRepository.findWeekEvents();
        for (MonthEvent weekEvent : weekEvents) {
            if (now.isAfter(weekEvent.getMonthEventDeadline().plusHours(5))) {
                LocalDateTime newDeadline = weekEvent.getMonthEventDeadline().plusDays(weekEvent.getExtendDay());
                monthEventRepository.updateWeekEventDeadline(newDeadline, weekEvent.getMonthEventId());
            }
        }
    }
}
