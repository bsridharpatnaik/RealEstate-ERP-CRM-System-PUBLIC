package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class IndentStatusUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;

    @Transactional(rollbackFor = Exception.class)
    public void updateIndentStatuses(Set<PurchaseOrderLine> lines, POIndentUpdateAction action) {
        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                updateSingleIndentLine(ref.getIndentLineItemCode(), action, line.getPurchaseOrder().getPurchaseOrderId());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateSingleIndentLine(String indentLineItemCode, POIndentUpdateAction action, String puchaseOrderId) {
        List<IndentInventoryList> items = indentInventoryListRepo.findByLineItemCode(indentLineItemCode);
        if (items.isEmpty()) {
            throw new RuntimeException("Indent line item not found: " + indentLineItemCode);
        }
        IndentInventoryList item = items.get(0);
        IndentInventory indent = item.getIndentInventory();
        if (action == POIndentUpdateAction.CREATE_PO) {
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
        } else if (action == POIndentUpdateAction.CANCEL_PO) {
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
        }
        if (action.equals(POIndentUpdateAction.CREATE_PO))
            item.setPurchaseOrderId(puchaseOrderId);
        else
            item.setPurchaseOrderId(null);
        recalculateIndentStatus(indent);
        indentInventoryListRepo.saveAll(indent.getInventoryList());
    }

    private void recalculateIndentStatus(IndentInventory indent) {
        boolean allPoCreated = indent.getInventoryList().stream().allMatch(i -> IndentLineItemStatusConstants.STATUS_PO_CREATED.equalsIgnoreCase(i.getLineItemStatus()));
        boolean nonePoCreated = indent.getInventoryList().stream().noneMatch(i -> IndentLineItemStatusConstants.STATUS_PO_CREATED.equalsIgnoreCase(i.getLineItemStatus()));
        if (allPoCreated) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_PO_COMPLETED);
        } else if (nonePoCreated) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_APPROVED);
        } else {
            indent.setIndentStatus(IndentStatusConstants.STATUS_PO_PARTIAL);
        }
    }
}