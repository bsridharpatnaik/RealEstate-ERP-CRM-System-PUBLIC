package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.*;
import com.ec.application.repository.IndentStatusHistoryRepo;
import com.ec.application.repository.PurchaseOrderStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

@Service
@UseDefaultTenant
public class PurchaseOrderStatusHistoryService {

    @Autowired
    private PurchaseOrderStatusHistoryRepo purchaseOrderStatusHistoryRepo;

    @Transactional
    public void logStatusChange(PurchaseOrder po, String oldStatus, String newStatus, String changedBy, String changeMessage) {
        if (!Objects.equals(oldStatus, newStatus)) {
            PurchaseOrderStatusHistory h = new PurchaseOrderStatusHistory();
            h.setPurchaseOrder(po);
            h.setOldStatus(oldStatus);   // can be null on creation
            h.setNewStatus(newStatus);   // must not be null
            h.setChangedAt(new Date());
            h.setChangedBy(changedBy);
            h.setChangeMessage(changeMessage);
            purchaseOrderStatusHistoryRepo.save(h);
        }
    }
}
