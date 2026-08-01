package com.hoaphat.pvt.repository.priority;

import com.hoaphat.pvt.model.priority.DailyPrioritySelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IDailyPrioritySelectionRepository extends JpaRepository<DailyPrioritySelection, Integer> {
    Optional<DailyPrioritySelection> findByAccount_AccountNameAndPriorityDate(
            String accountName, LocalDate priorityDate);

    void deleteByAccount_AccountName(String accountName);
}
