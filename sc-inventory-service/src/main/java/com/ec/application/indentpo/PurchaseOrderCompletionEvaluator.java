package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderCompletionEvaluator {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final IndentInventoryListRepo indentInventoryListRepo;

    @Transactional
    public void evaluate(PurchaseOrder po) {

        // --------------------------------------------
        // Collect ALL indent line item codes
        // --------------------------------------------
        Set<String> lineItemCodes = new HashSet<String>();

        for (PurchaseOrderLine poLine : po.getLines()) {
            if (poLine.getIndentRefs() == null) {
                continue;
            }

            for (PurchaseOrderIndentRef ref : poLine.getIndentRefs()) {
                if (ref.getIndentLineItemCode() != null) {
                    lineItemCodes.add(ref.getIndentLineItemCode());
                }
            }
        }

        if (lineItemCodes.isEmpty()) {
            return;
        }

        // --------------------------------------------
        // Fetch indent lines in ONE DB call
        // --------------------------------------------
        List<IndentInventoryList> indentLines = indentInventoryListRepo.findByLineItemCodeIn(lineItemCodes);

        if (indentLines.isEmpty()) {
            return;
        }

        // --------------------------------------------
        // Evaluate PO status
        // --------------------------------------------
        boolean anyInwardStarted = false;
        boolean allInwardComplete = true;

        for (IndentInventoryList indentLine : indentLines) {

            String status = indentLine.getLineItemStatus();

            // Has any inward started?
            if (IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL.equals(status)
                    || IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE.equals(status)) {
                anyInwardStarted = true;
            }

            // Are all inward complete?
            if (!IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE.equals(status)) {
                allInwardComplete = false;
            }
        }

        if (!anyInwardStarted) {
            // No inward done for any indent line
            po.setStatus(POStatusConstants.STATUS_NEW);
        } else if (allInwardComplete) {
            // All indent lines fully inwarded
            po.setStatus(POStatusConstants.STATUS_COMPLETED);
        } else {
            // Some inward done, but not all complete
            po.setStatus(POStatusConstants.STATUS_PARTIAL);
        }
        purchaseOrderRepo.save(po);
    }
}
