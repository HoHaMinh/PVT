package com.hoaphat.pvt.repository;

import com.hoaphat.pvt.model.event.ResponseEventInformation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IResponseEventInformationRepository extends CrudRepository<ResponseEventInformation,Integer> {
    @Query("select r from ResponseEventInformation r where r.monthEvent.monthEventId = :idMonthEvent order by r.createdByDate asc ")
    List<ResponseEventInformation> findResponseById(Integer idMonthEvent);
}
