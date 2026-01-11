package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class IndentStatusUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;

    public void updateIndentStatuses(Set<PurchaseOrderLine> lines) {

        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                updateSingleIndentLine(ref.getIndentLineItemCode());
            }
        }
    }

    private void updateSingleIndentLine(String indentLineItemCode) {
        List<IndentInventoryList> items = indentInventoryListRepo.findByLineItemCode(indentLineItemCode);
        if (items.isEmpty())
            throw new RuntimeException("Indent line item not found: " + indentLineItemCode);
        IndentInventoryList item = items.get(0);
        item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
        IndentInventory indent = item.getIndentInventory();
        boolean allPoCreated = indent.getInventoryList().stream()
                .allMatch(i ->
                        IndentLineItemStatusConstants.STATUS_PO_CREATED
                                .equalsIgnoreCase(i.getLineItemStatus())
                );
        indent.setIndentStatus(allPoCreated ? IndentStatusConstants.STATUS_PO_COMPLETED : IndentStatusConstants.STATUS_PO_PARTIAL);
        indentInventoryListRepo.saveAll(indent.getInventoryList());
    }
}