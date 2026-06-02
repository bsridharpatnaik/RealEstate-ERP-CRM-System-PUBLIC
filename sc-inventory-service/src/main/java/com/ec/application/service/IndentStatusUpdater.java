package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.HistoryRelationType;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.data.HistoryRelationInput;
import com.ec.application.indentpo.IndentCompletionEvaluator;
import com.ec.application.model.*;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@UseDefaultTenant
public class IndentStatusUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final IndentStatusHistoryService indentStatusHistoryService;

    /**
     * Marks a single indent line item as PO CREATED and links it to the given PO.
     * Used when adding a new line item to an existing PO.
     */
    @Transactional
    public void markIndentLineAsPOCreated(String lineItemCode, String poId) {
        IndentInventoryList item = indentInventoryListRepo.findByLineItemCode(lineItemCode).get(0);
        item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
        item.setPurchaseOrderId(poId);
        indentStatusHistoryService.logStatusChange(
            item.getIndentInventory(), null, null, "System",
            "Indent line item " + lineItemCode + " status changed to PO CREATED — line item added to PO " + poId + ".",
            Collections.singletonList(new HistoryRelationInput(HistoryRelationType.PO, "", poId)));
        indentInventoryListRepo.save(item);
        indentCompletionEvaluator.evaluate(item.getIndentInventory());
    }

    /**
     * Reverts a single indent line item back to NEW and clears its PO linkage.
     * Used when removing an open line item from an existing PO.
     */
    @Transactional
    public void revertIndentLineToNew(String lineItemCode, String poId) {
        IndentInventoryList item = indentInventoryListRepo.findByLineItemCode(lineItemCode).get(0);
        item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
        item.setPurchaseOrderId(null);
        indentStatusHistoryService.logStatusChange(
            item.getIndentInventory(), null, null, "System",
            "Indent line item " + lineItemCode + " reverted to NEW — line item removed from PO " + poId + ".",
            Collections.singletonList(new HistoryRelationInput(HistoryRelationType.PO, "", poId)));
        indentInventoryListRepo.save(item);
        indentCompletionEvaluator.evaluate(item.getIndentInventory());
    }

    @Transactional
    public void updateIndentStatuses(PurchaseOrder po, POIndentUpdateAction action) {

        for (PurchaseOrderLine line : po.getLines()) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                IndentInventoryList item = indentInventoryListRepo.findByLineItemCode(ref.getIndentLineItemCode()).get(0);

                if (action == POIndentUpdateAction.CREATE_PO) {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
                    item.setPurchaseOrderId(po.getPurchaseOrderId());
                    indentStatusHistoryService.logStatusChange(item.getIndentInventory(), null, null, "System", "Indent line item " + item.getLineItemCode() + " status changed to " + item.getLineItemStatus() + " due to PO " + "creation " + po.getPurchaseOrderId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.PO, "", po.getPurchaseOrderId())));
                } else {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
                    item.setPurchaseOrderId(null);
                    indentStatusHistoryService.logStatusChange(item.getIndentInventory(), null, null, "System", "Indent line item " + item.getLineItemCode() + " status changed to " + item.getLineItemStatus() + " due to PO " + "cancellation." + po.getPurchaseOrderId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.PO, "", po.getPurchaseOrderId())));
                }
                indentInventoryListRepo.save(item);
                indentCompletionEvaluator.evaluate(item.getIndentInventory());
            }
        }
    }
}
