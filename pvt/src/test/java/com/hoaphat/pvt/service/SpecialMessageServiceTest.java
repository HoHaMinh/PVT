package com.hoaphat.pvt.service;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.HomeCelebration;
import com.hoaphat.pvt.model.event.SpecialMessage;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.ISpecialMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialMessageServiceTest {
    @Mock
    private ISpecialMessageRepository specialMessageRepository;

    @Mock
    private IAccountRepository accountRepository;

    @InjectMocks
    private SpecialMessageService service;

    @Test
    void oneBirthdayUsesSingularWordingAndFullName() {
        LocalDate today = LocalDate.of(2026, 7, 6);
        Account account = new Account("nva", "ignored", "Nguyễn Văn An");
        when(accountRepository.findByBirthdayOrderByNameAsc("06/07"))
                .thenReturn(Collections.singletonList(account));
        when(specialMessageRepository.findAll()).thenReturn(Collections.emptyList());

        HomeCelebration result = service.getTodayCelebration(today);

        assertNotNull(result);
        assertTrue(result.hasBirthdays());
        assertFalse(result.getBirthdayIntroduction().contains("các thành viên"));
        assertEquals(Collections.singletonList("Nguyễn Văn An"), result.getBirthdayNames());
    }

    @Test
    void duplicateBirthdayUsesPluralWordingAndKeepsAllFullNames() {
        LocalDate today = LocalDate.of(2026, 7, 6);
        Account first = new Account("nva", "ignored", "Nguyễn Văn An");
        Account second = new Account("ttb", "ignored", "Trần Thị Bình");
        when(accountRepository.findByBirthdayOrderByNameAsc("06/07"))
                .thenReturn(Arrays.asList(first, second));
        when(specialMessageRepository.findAll()).thenReturn(Collections.emptyList());

        HomeCelebration result = service.getTodayCelebration(today);

        assertTrue(result.getBirthdayIntroduction().contains("các thành viên"));
        assertEquals(Arrays.asList("Nguyễn Văn An", "Trần Thị Bình"),
                result.getBirthdayNames());
    }

    @Test
    void birthdayAndWomensDayAreBothDisplayedOnTheSameDate() {
        LocalDate today = LocalDate.of(2026, 3, 8);
        Account account = new Account("nva", "ignored", "Nguyễn Văn An");
        SpecialMessage womensDay = new SpecialMessage(10,
                "Chúc chị em luôn xinh đẹp và hạnh phúc.",
                LocalDateTime.of(2020, 3, 8, 0, 0));
        when(accountRepository.findByBirthdayOrderByNameAsc("08/03"))
                .thenReturn(Collections.singletonList(account));
        when(specialMessageRepository.findAll()).thenReturn(Collections.singletonList(womensDay));

        HomeCelebration result = service.getTodayCelebration(today);

        assertTrue(result.hasBirthdays());
        assertTrue(result.hasSpecialMessages());
        assertEquals("Mừng Ngày Quốc tế Phụ nữ 08/03",
                result.getSpecialMessages().get(0).getDisplayTitle());
    }

    @Test
    void returnsNullWhenNoBirthdayAndNoUsableSpecialMessage() {
        LocalDate today = LocalDate.of(2026, 7, 6);
        SpecialMessage blank = new SpecialMessage(11, "   ",
                LocalDateTime.of(2020, 7, 6, 0, 0));
        when(accountRepository.findByBirthdayOrderByNameAsc("06/07"))
                .thenReturn(Collections.emptyList());
        when(specialMessageRepository.findAll()).thenReturn(Collections.singletonList(blank));

        assertNull(service.getTodayCelebration(today));
    }
}
