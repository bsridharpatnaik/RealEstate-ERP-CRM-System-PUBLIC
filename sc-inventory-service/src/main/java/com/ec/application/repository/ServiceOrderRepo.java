package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.ServiceOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepo extends BaseRepository<ServiceOrder, String> {

    // Note: cannot also eager-fetch "lines.customFields" here — Hibernate throws
    // MultipleBagFetchException when fetch-joining two List ("bag") collections in one query.
    // customFields is force-initialized separately in ServiceOrderService.getServiceOrderWithInit().
    @EntityGraph(attributePaths = {"vendor", "firm", "lines"})
    @Query("select so from ServiceOrder so where so.serviceOrderId = :id")
    Optional<ServiceOrder> findByIdWithDetails(@Param("id") String id);

    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.vendor.contactId = :id")
    int vendorUsageCount(@Param("id") Long id);

    @Query("SELECT DISTINCT l.description FROM ServiceOrderLine l WHERE l.isDeleted = false AND l.description IS NOT NULL ORDER BY l.description ASC")
    List<String> findDistinctLineDescriptions();

    @Query("SELECT DISTINCT cf.fieldLabel FROM ServiceOrderLineCustomField cf WHERE cf.fieldLabel IS NOT NULL ORDER BY cf.fieldLabel ASC")
    List<String> findDistinctCustomFieldLabels();

    // ── Next-service-date reminder tile counts ──────────────────────────────
    // nextServiceDate is only ever set on COMPLETED orders (see markComplete()); excluding
    // CANCELLED is defensive since cancelled orders never get a nextServiceDate set anyway.

    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.nextServiceDate IS NOT NULL AND s.status <> 'CANCELLED' AND s.nextServiceDate < :today")
    long countNextServiceOverdue(@Param("today") java.util.Date today);

    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.nextServiceDate IS NOT NULL AND s.status <> 'CANCELLED' AND s.nextServiceDate BETWEEN :from AND :to")
    long countNextServiceBetween(@Param("from") java.util.Date from, @Param("to") java.util.Date to);
}
