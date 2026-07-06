package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.dto.HomeCelebration;

import java.time.LocalDate;

public interface ISpecialMessageService {
    HomeCelebration getTodayCelebration(LocalDate today);
}
