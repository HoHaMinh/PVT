package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SercuritySchedule;
import com.hoaphat.pvt.repository.event.ISercurityScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SercurityScheduleService implements ISercurityScheduleService{
    @Autowired
    private ISercurityScheduleRepository sercurityScheduleRepository;

    @Override
    public List<SercuritySchedule> getAll() {
        return (List<SercuritySchedule>) sercurityScheduleRepository.findAll();
    }
}
