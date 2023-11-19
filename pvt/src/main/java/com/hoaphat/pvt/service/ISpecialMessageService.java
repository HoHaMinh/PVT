package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.event.SpecialMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ISpecialMessageService {
    SpecialMessage getAll(LocalDateTime now);
}
