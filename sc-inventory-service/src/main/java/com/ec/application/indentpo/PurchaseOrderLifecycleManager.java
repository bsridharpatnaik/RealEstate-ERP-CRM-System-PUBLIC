package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.HistoryRelationType;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.HistoryRelationInput;
import com.ec.application.data.ShortClosePoRequest;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderStatusHistory;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.repository.PurchaseOrderStatusHistoryRepo;
import com.ec.application.service.IndentStatusHistoryService;
import com.ec.application.service.IndentStatusUpdater;
import com.ec.application.service.PurchaseOrderStatusHistoryService;
import com.ec.application.service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderLifecycleManager {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final IndentStatusUpdater indentStatusUpdater;
    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final IndentStatusHistoryService indentStatusHistoryService;
    private final PurchaseOrderStatusHistoryService purchaseOrderStatusHistoryService;
    private final UserDetailsService userDetailsService;

    @Transactional
    public void cancelIfAllowed(String poId) throws Exception {

        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new IllegalStateException("PO not found"));

        if (!po.getStatus().equalsIgnoreCase(POStatusConstants.STATUS_NEW)) {
            throw new IllegalStateException("PO can be cancelled only in status NEW. It must be short closed.");
        }

        purchaseOrderStatusHistoryService.logStatusChange(po, po.getStatus(), POStatusConstants.STATUS_CANCELLED, "System", "PO cancelled by user " + userDetailsService.getCurrentUser().getUsername(), null);
        po.setLastStatusUpdatedAt(new Date());
        po.setStatus(POStatusConstants.STATUS_CANCELLED);
        purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(po, POIndentUpdateAction.CANCEL_PO);
    }

    @Transactional
    public void shortClosePo(ShortClosePoRequest request) throws Exception {

        PurchaseOrder po = purchaseOrderRepo.findById(request.getPurchaseOrderNo())
                .orElseThrow(() -> new IllegalStateException("PO not found"));

        // 1. PO must not be CANCELLED or CLOSED
        if (!POStatusConstants.STATUS_PARTIAL.equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException("PO cannot be short closed in status " + po.getStatus());
        }

        // 3. Update PO status
        purchaseOrderStatusHistoryService.logStatusChange(po, po.getStatus(), POStatusConstants.STATUS_SHORT_CLOSED, "System", "PO short closed by user " + userDetailsService.getCurrentUser().getUsername(), null);
        po.setLastStatusUpdatedAt(new Date());
        po.setStatus(POStatusConstants.STATUS_SHORT_CLOSED);
        po.setShortCloseReason(request.getReason()); // optional column
        purchaseOrderRepo.save(po);

        // 4. Short close remaining indent line items of this PO
        po.getLines().forEach(line ->
                line.getIndentRefs().forEach(ref -> {
                    IndentInventoryList item = indentInventoryListRepo.findByLineItemCode(ref.getIndentLineItemCode()).get(0);
                    // Only pending items are short closed
                    if (IndentLineItemStatusConstants.SHORTCLOSE_ALLOWED_STATUSES.contains(item.getLineItemStatus())) {
                        item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_SHORT_CLOSED);
                        indentStatusHistoryService.logStatusChange(item.getIndentInventory(), null, null, "System", "Indent line item " + item.getLineItemCode() + " status changed to " + item.getLineItemStatus() + " due to PO " + " short close - " + po.getPurchaseOrderId() +".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.PO, "", po.getPurchaseOrderId())));
                        indentInventoryListRepo.save(item);
                    }
                })
        );

        // 5. Recalculate indent statuses
        po.getLines().forEach(line ->
                line.getIndentRefs().forEach(ref -> {
                    IndentInventory indent =
                            indentInventoryListRepo
                                    .findByLineItemCode(ref.getIndentLineItemCode())
                                    .get(0)
                                    .getIndentInventory();
                    indentCompletionEvaluator.evaluate(indent);
                })
        );
    }
}
