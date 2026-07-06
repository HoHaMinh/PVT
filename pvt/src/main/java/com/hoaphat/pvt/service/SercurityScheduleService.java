package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SercuritySchedule;
import com.hoaphat.pvt.repository.event.ISercurityScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SercurityScheduleService implements ISercurityScheduleService{
    @Autowired
    private ISercurityScheduleRepository sercurityScheduleRepository;

    @Override
    public List<SercuritySchedule> getAll() {
        List<SercuritySchedule> result = new ArrayList<>();
        for (SercuritySchedule schedule : sercurityScheduleRepository.findAll()) {
            if (hasContent(schedule)) {
                result.add(schedule);
            }
        }
        return result;
    }

    private boolean hasContent(SercuritySchedule schedule) {
        return schedule != null && (hasText(schedule.getStr1())
                || hasText(schedule.getStr2())
                || hasText(schedule.getStr3())
                || hasText(schedule.getStr4()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
