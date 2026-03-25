package com.hoaphat.pvt.repository.event;

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
    //    *Trang private
    @Query("SELECT m FROM MonthEvent m " +
            "WHERE m.monthEventDeadline > :before " +
            "AND m.monthEventDeadline < :after " +
            "AND m.hidden = false " +
            "AND (m.all = true OR m.account.accountName = :accountName) " +
            "ORDER BY m.monthEventDeadline, m.monthEventId ASC")
    List<MonthEvent> findByAccountFilter(LocalDateTime before,
                                         LocalDateTime after,
                                         String accountName);

    //    *Trang response
    //    Update status = 1 khi là quản lý phản hồi
    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.responseStatus = 1 where m.monthEventId = :id")
    void updateResponseStatus1(Integer id);

    //    Update status = 2 khi là nhân viên phản hồi
    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.responseStatus = 2 where m.monthEventId = :id")
    void updateResponseStatus2(Integer id);

    //    *Trang task
    @Query(value = "select m from MonthEvent m where m.monthEventStatus = 0 or m.monthEventStatus = 1 order by m.monthEventDeadline, m.monthEventId asc",
            countQuery = "select count(m) from MonthEvent m where m.monthEventStatus = 0 or m.monthEventStatus = 1 order by m.monthEventDeadline, m.monthEventId asc")
    Page<MonthEvent> findMonthEventsByMonthEventStatus(Pageable pageable);

    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.monthEventDeadline = :now where m.monthEventId = :id")
    void updateWeekEventDeadline(LocalDateTime now, Integer id);

    //    *Trang weekly task
    @Query("select m from MonthEvent m where m.monthEventStatus = 2 order by m.monthEventDeadline, m.monthEventId asc ")
    List<MonthEvent> findWeekEvents();

    @Transactional
    @Modifying
    @Query("update MonthEvent m set m.lastTimeResponse = :lastTimeResponse, m.lastPersonResponse = :lastPersonResponse  where m.monthEventId = :id")
    void updateMonthEventResponse(LocalDateTime lastTimeResponse, String lastPersonResponse, Integer id);

    // ✅ Chỉ lấy m.all = true, không lấy việc cá nhân
    @Query("SELECT m FROM MonthEvent m " +
            "WHERE m.monthEventDeadline > :before " +
            "AND m.monthEventDeadline < :after " +
            "AND m.all = true " +
            "ORDER BY m.monthEventDeadline, m.monthEventId ASC")
    List<MonthEvent> findAllFilter(LocalDateTime before, LocalDateTime after);

    void deleteByAccount_AccountName(String accountName);
}
