package com.ec.application.enricher;

import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PurchaseOrderUiEnricher {

    @Autowired
    UserDetailsService userDetailsService;

    public void enrich(PurchaseOrder po) {
        if (po == null) return;

        try {
            boolean allowed = userDetailsService.isAdminOrManager() && POStatusConstants.STATUS_NEW.equals(po.getStatus());
            po.setApprovalAllowed(true);
            po.setCancellationAllowed(true);
        } catch (Exception e) {
            // fail-safe: UI flag should never break response
            po.setApprovalAllowed(false);
            po.setCancellationAllowed(false);
        }
    }

    public void enrich(Collection<PurchaseOrder> pos) {
        if (pos == null) return;
        pos.forEach(this::enrich);
    }
}
