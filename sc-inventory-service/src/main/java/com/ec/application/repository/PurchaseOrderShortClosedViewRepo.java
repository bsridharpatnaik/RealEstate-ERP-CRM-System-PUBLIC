package com.ec.application.repository;

import com.ec.application.model.PurchaseOrderShortClosedView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderShortClosedViewRepo extends JpaRepository<PurchaseOrderShortClosedView, String> {
}
