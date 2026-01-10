package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.PurchaseOrder;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepo extends BaseRepository<PurchaseOrder, Long> {
}