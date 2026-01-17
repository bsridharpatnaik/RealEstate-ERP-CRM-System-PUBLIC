package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.indentpo.IndentCompletionEvaluator;
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

@RequiredArgsConstructor
@Service
@UseDefaultTenant
public class IndentStatusUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;

    @Transactional
    public void updateIndentStatuses(Set<PurchaseOrderLine> lines, POIndentUpdateAction action) {

        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {

                IndentInventoryList item =
                        indentInventoryListRepo.findByLineItemCode(
                                ref.getIndentLineItemCode()
                        ).get(0);

                if (action == POIndentUpdateAction.CREATE_PO) {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_PO_CREATED);
                } else {
                    item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
                }

                indentInventoryListRepo.save(item);
                indentCompletionEvaluator.evaluate(item.getIndentInventory());
            }
        }
    }
}
