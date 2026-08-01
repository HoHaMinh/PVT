package com.hoaphat.pvt.repository.event;

import com.hoaphat.pvt.model.event.ResponseAttachment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IResponseAttachmentRepository extends CrudRepository<ResponseAttachment, Integer> {
    @Query("select a from ResponseAttachment a " +
            "join fetch a.response r " +
            "join fetch r.monthEvent m " +
            "where a.idAttachment = :idAttachment " +
            "and (m.deleted = false or m.deleted is null)")
    Optional<ResponseAttachment> findActiveAttachment(Integer idAttachment);

    @Query("select a from ResponseAttachment a " +
            "join fetch a.response r " +
            "where r.idResponse in :responseIds " +
            "order by a.idAttachment asc")
    List<ResponseAttachment> findByResponseIds(Collection<Integer> responseIds);

    @Query("select a from ResponseAttachment a " +
            "join a.response r " +
            "where r.monthEvent.monthEventId = :monthEventId")
    List<ResponseAttachment> findByMonthEventId(Integer monthEventId);
}
