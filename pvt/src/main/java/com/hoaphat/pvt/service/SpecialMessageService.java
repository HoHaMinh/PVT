package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SpecialMessage;
import com.hoaphat.pvt.repository.event.ISpecialMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SpecialMessageService implements ISpecialMessageService{
    @Autowired
    private ISpecialMessageRepository specialMessageRepository;

    @Override
    public SpecialMessage getAll(LocalDateTime now) {
        List<SpecialMessage> specialMessageList = (List<SpecialMessage>) specialMessageRepository.findAll();
        for (SpecialMessage s: specialMessageList
             ) {
            if (s.getBirthday().getMonthValue() == now.getMonthValue() && s.getBirthday().getDayOfMonth() == now.getDayOfMonth()) {
                return s;
            }
        }
       return null;
    }
}
