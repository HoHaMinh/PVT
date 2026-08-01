package com.hoaphat.pvt.repository.event;

import com.hoaphat.pvt.model.event.ResponseEventInformation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface IResponseEventInformationRepository extends CrudRepository<ResponseEventInformation, Integer> {
    @Query("select r from ResponseEventInformation r where r.monthEvent.monthEventId = :idMonthEvent " +
            "and (r.monthEvent.deleted = false or r.monthEvent.deleted is null) order by r.createdByDate desc ")
    List<ResponseEventInformation> findResponseById(Integer idMonthEvent);

    @Query("select r from ResponseEventInformation r " +
            "join fetch r.monthEvent m " +
            "where r.createdByUser = :accountName " +
            "and r.createdByDate >= :startInclusive " +
            "and r.createdByDate < :endExclusive " +
            "and (m.deleted = false or m.deleted is null) " +
            "and (m.hidden = false or m.hidden is null) " +
            "order by m.monthEventDeadline asc, m.monthEventId asc, " +
            "r.createdByDate asc, r.idResponse asc")
    List<ResponseEventInformation> findForEmployeeSummary(
            String accountName, LocalDateTime startInclusive, LocalDateTime endExclusive);

    @Transactional
    @Modifying
    @Query("delete from ResponseEventInformation r where r.monthEvent.monthEventId = :idMonthEvent")
    void deleteResponseById(Integer idMonthEvent);

    void deleteByMonthEvent_MonthEventIdAndEventInformationResponseStartingWith(
            Integer monthEventId, String prefix
    );

}
