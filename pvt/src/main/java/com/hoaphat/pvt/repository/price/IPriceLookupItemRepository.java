package com.hoaphat.pvt.repository.price;

import com.hoaphat.pvt.model.price.PriceLookupItem;
import com.hoaphat.pvt.model.price.PriceLookupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;

public interface IPriceLookupItemRepository extends JpaRepository<PriceLookupItem, Integer> {
    @EntityGraph(attributePaths = "candidates")
    List<PriceLookupItem> findByStatusInAndAssignedAccountNameOrderByCreatedAtAsc(
            Collection<PriceLookupStatus> statuses, String assignedAccountName);

    @EntityGraph(attributePaths = "candidates")
    List<PriceLookupItem> findByStatusInOrderByCreatedAtAsc(Collection<PriceLookupStatus> statuses);

    boolean existsByStatusInAndAssignedAccountName(
            Collection<PriceLookupStatus> statuses, String assignedAccountName);

    @Query("select distinct i.assignedAccountName from PriceLookupItem i "
            + "where i.status in :statuses")
    List<String> findDistinctAssignedAccountNamesByStatusIn(
            @Param("statuses") Collection<PriceLookupStatus> statuses);

    @EntityGraph(attributePaths = "candidates")
    Optional<PriceLookupItem> findWithDetailsByItemId(Integer itemId);

    @Query(value = "select distinct i from PriceLookupItem i left join i.results r "
            + "where i.status = :status "
            + "and (i.deleted = false or i.deleted is null) "
            + "and (:accountName = '' or i.assignedAccountName = :accountName) "
            + "and (:search = '' or lower(coalesce(i.displayName, i.keyword)) like lower(concat('%', :search, '%')) "
            + "or lower(r.supplierName) like lower(concat('%', :search, '%'))) "
            + "order by i.completedAt desc",
            countQuery = "select count(distinct i) from PriceLookupItem i left join i.results r "
                    + "where i.status = :status "
                    + "and (i.deleted = false or i.deleted is null) "
                    + "and (:accountName = '' or i.assignedAccountName = :accountName) "
                    + "and (:search = '' or lower(coalesce(i.displayName, i.keyword)) like lower(concat('%', :search, '%')) "
                    + "or lower(r.supplierName) like lower(concat('%', :search, '%')))")
    Page<PriceLookupItem> searchCompleted(@Param("status") PriceLookupStatus status,
                                          @Param("accountName") String accountName,
                                          @Param("search") String search,
                                          Pageable pageable);

    @Query(value = "select distinct i from PriceLookupItem i left join i.results r "
            + "where i.status = :status and i.deleted = true "
            + "and (:accountName = '' or i.assignedAccountName = :accountName) "
            + "and (:search = '' or lower(coalesce(i.displayName, i.keyword)) like lower(concat('%', :search, '%')) "
            + "or lower(r.supplierName) like lower(concat('%', :search, '%'))) "
            + "order by i.completedAt desc",
            countQuery = "select count(distinct i) from PriceLookupItem i left join i.results r "
                    + "where i.status = :status and i.deleted = true "
                    + "and (:accountName = '' or i.assignedAccountName = :accountName) "
                    + "and (:search = '' or lower(coalesce(i.displayName, i.keyword)) like lower(concat('%', :search, '%')) "
                    + "or lower(r.supplierName) like lower(concat('%', :search, '%')))")
    Page<PriceLookupItem> searchDeletedCompleted(@Param("status") PriceLookupStatus status,
                                                 @Param("accountName") String accountName,
                                                 @Param("search") String search,
                                                 Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PriceLookupItem i where i.deleted = true order by i.itemId")
    List<PriceLookupItem> findDeletedForUpdate();
}
