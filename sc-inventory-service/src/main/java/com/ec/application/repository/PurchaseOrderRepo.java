package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.PurchaseOrder;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends BaseRepository<PurchaseOrder, String> {
    @EntityGraph(attributePaths = {
            "supplier",
            "firm",
            "lines",
            "lines.product",
            "lines.indentRefs"
    })
    @Query("SELECT po FROM PurchaseOrder po WHERE po.purchaseOrderId = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") String id);
}