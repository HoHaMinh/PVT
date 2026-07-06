package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.HomeCelebration;
import com.hoaphat.pvt.model.event.SpecialMessage;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.ISpecialMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SpecialMessageService implements ISpecialMessageService{
    @Autowired
    private ISpecialMessageRepository specialMessageRepository;
    @Autowired
    private IAccountRepository accountRepository;

    @Override
    public HomeCelebration getTodayCelebration(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Ngày kiểm tra lời chúc không được để trống.");
        }

        String birthdayKey = today.format(DateTimeFormatter.ofPattern("dd/MM"));
        List<Account> birthdayAccounts = accountRepository.findByBirthdayOrderByNameAsc(birthdayKey);
        Set<String> uniqueNames = new LinkedHashSet<>();
        if (birthdayAccounts != null) {
            for (Account account : birthdayAccounts) {
                if (account == null) continue;
                String fullName = account.getName();
                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = account.getAccountName();
                }
                if (fullName != null && !fullName.trim().isEmpty()) {
                    uniqueNames.add(fullName.trim().replaceAll("\\s+", " "));
                }
            }
        }

        List<SpecialMessage> specialMessages = new ArrayList<>();
        for (SpecialMessage specialMessage : specialMessageRepository.findAll()) {
            if (specialMessage == null || specialMessage.getBirthday() == null
                    || specialMessage.getMessage() == null
                    || specialMessage.getMessage().trim().isEmpty()) {
                continue;
            }
            if (specialMessage.getBirthday().getMonthValue() == today.getMonthValue()
                    && specialMessage.getBirthday().getDayOfMonth() == today.getDayOfMonth()) {
                specialMessages.add(specialMessage);
            }
        }
        specialMessages.sort(Comparator.comparing(SpecialMessage::getId,
                Comparator.nullsLast(Integer::compareTo)));

        if (uniqueNames.isEmpty() && specialMessages.isEmpty()) {
            return null;
        }
        return new HomeCelebration(today, new ArrayList<>(uniqueNames), specialMessages);
    }
}
