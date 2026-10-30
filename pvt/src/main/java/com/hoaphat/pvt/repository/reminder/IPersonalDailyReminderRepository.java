package com.hoaphat.pvt.repository.reminder;

import com.hoaphat.pvt.model.reminder.PersonalDailyReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IPersonalDailyReminderRepository
        extends JpaRepository<PersonalDailyReminder, Integer> {
    boolean existsByReminderDate(LocalDate reminderDate);
    Optional<PersonalDailyReminder> findByReminderDateAndAccountName(
            LocalDate reminderDate, String accountName);
    void deleteByReminderDateBefore(LocalDate cutoffDate);
}
