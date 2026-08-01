package com.hoaphat.pvt.repository.priority;

import com.hoaphat.pvt.model.priority.TaskPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface ITaskPriorityRepository extends JpaRepository<TaskPriority, Integer> {
    List<TaskPriority> findBySelection_SelectionId(Integer selectionId);

    void deleteBySelection_SelectionId(Integer selectionId);

    void deleteBySelection_Account_AccountName(String accountName);

    void deleteByMonthEvent_MonthEventId(Integer monthEventId);

    void deleteByMonthEvent_MonthEventIdAndSelection_PriorityDate(
            Integer monthEventId, LocalDate priorityDate);
}
