package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.indentpo.IndentCompletionEvaluator;
import com.ec.application.model.*;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@UseDefaultTenant
public class IndentStatusUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final IndentStatusHistoryService indentStatusHistoryService;

    @Transactional
    public void updateIndentStatuses(PurchaseOrder po, POIndentUpdateAction action) {

        for (PurchaseOrderLine line : po.getLines()) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                IndentInventoryList item = indentInventoryListRepo.findByLineItemCode(ref.getIndentLineItemCode()).get(0);

                if (action == POIndentUpdateAction.CREATE_PO) {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
                    item.setPurchaseOrderId(po.getPurchaseOrderId());
                    indentStatusHistoryService.logStatusChange(item.getIndentInventory(), null, null, "System", "Indent line item " + item.getLineItemCode() + " status changed to " + item.getLineItemStatus() + " due to PO " + "creation " + po.getPurchaseOrderId() +".");
                } else {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
                    item.setPurchaseOrderId(null);
                    indentStatusHistoryService.logStatusChange(item.getIndentInventory(), null, null, "System", "Indent line item " + item.getLineItemCode() + " status changed to " + item.getLineItemStatus() + " due to PO " + "cancellation." + po.getPurchaseOrderId() +".");
                }
                indentInventoryListRepo.save(item);
                indentCompletionEvaluator.evaluate(item.getIndentInventory());
            }
        }
    }
}
