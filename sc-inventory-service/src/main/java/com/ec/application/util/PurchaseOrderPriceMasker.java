package com.ec.application.util;

import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.service.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PurchaseOrderPriceMasker {

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Mask price fields for a single PurchaseOrder
     */
    public void mask(PurchaseOrder po) throws Exception {
        if (po == null || !userDetailsService.isInventoryExecutive()) {
            return;
        }

        if (po.getLines() == null) {
            return;
        }

        for (PurchaseOrderLine line : po.getLines()) {
            po.setGrandTotal(null);
            maskLine(line);
        }
    }

    /**
     * Mask price fields for a collection of PurchaseOrders
     */
    public void mask(Collection<PurchaseOrder> purchaseOrders) throws Exception {
        if (purchaseOrders == null || !userDetailsService.isInventoryExecutive()) {
            return;
        }

        for (PurchaseOrder po : purchaseOrders) {
            mask(po);
            po.setGrandTotal(null);
        }
    }

    /**
     * Mask price fields for a Spring Page
     */
    public void mask(Page<PurchaseOrder> page) throws Exception {
        if (page == null || !userDetailsService.isInventoryExecutive()) {
            return;
        }
        mask(page.getContent());
    }

    private void maskLine(PurchaseOrderLine line) {
        if (line == null) return;
        line.setRate(null);
        line.setNetRate(null);
        line.setGstPercent(null);
        line.setTotalAmount(null);
    }
}
