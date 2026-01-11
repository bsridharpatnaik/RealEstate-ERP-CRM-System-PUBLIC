package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.PurchaseOrder;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends BaseRepository<PurchaseOrder, Long> {
    @Query("SELECT po FROM PurchaseOrder po " +
            "LEFT JOIN FETCH po.supplier " +
            "LEFT JOIN FETCH po.firm " +
            "LEFT JOIN FETCH po.lines l " +
            "LEFT JOIN FETCH l.product " +
            "LEFT JOIN FETCH l.indentRefs " +
            "WHERE po.purchaseOrderId = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") String id);
}