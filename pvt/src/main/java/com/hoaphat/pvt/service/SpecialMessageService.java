package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SpecialMessage;
import com.hoaphat.pvt.repository.ISpecialMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class SpecialMessageService implements ISpecialMessageService{
    @Autowired
    private ISpecialMessageRepository specialMessageRepository;

    @Override
    public List<SpecialMessage> getAll() {
       return (List<SpecialMessage>) specialMessageRepository.findAll();
    }
}
