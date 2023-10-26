package com.hoaphat.pvt.repository;

import com.hoaphat.pvt.model.event.MonthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IMonthEventRepository extends JpaRepository<MonthEvent, Integer> {
    @Query("select m from MonthEvent m where m.monthEventDeadline > :localDateTimeBefore and m.monthEventDeadline < :localDateTimeAfter order by m.monthEventDeadline asc ")
    List<MonthEvent> findMonthEventsByTime(LocalDateTime localDateTimeBefore, LocalDateTime localDateTimeAfter);

    @Query("select m from MonthEvent m where m.monthEventDeadline > :localDateTimeBefore and m.monthEventDeadline < :localDateTimeAfter " +
            "and ((m.monthEventDescription LIKE CONCAT(:name, '%') or :name is null) or m.monthEventDescription LIKE CONCAT('Cả phòng', '%')) order by m.monthEventDeadline asc ")
    List<MonthEvent> findMonthEventsByFilter(LocalDateTime localDateTimeBefore, LocalDateTime localDateTimeAfter, String name);

    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.monthEventStatus = 1 where m.monthEventDeadline < :now")
    void updateMonthEventStatus(LocalDateTime now);

    @Transactional
    @Modifying
    @Query("delete MonthEvent m where m.monthEventStatus = 1")
    void deleteMonthEventByStatus();

    @Query("select m from MonthEvent m where m.monthEventStatus = 2 order by m.monthEventDeadline asc ")
    List<MonthEvent> findWeekEvents();

    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.monthEventDeadline = :now where m.monthEventId = :id")
    void updateWeekEventDeadline(LocalDateTime now, Integer id);

    @Query("select m from MonthEvent m where m.monthEventStatus = 2 ")
    List<MonthEvent> findAllWeekEvents();

    Page<MonthEvent> findMonthEventsByMonthEventStatus(Integer status, Pageable pageable);
}
