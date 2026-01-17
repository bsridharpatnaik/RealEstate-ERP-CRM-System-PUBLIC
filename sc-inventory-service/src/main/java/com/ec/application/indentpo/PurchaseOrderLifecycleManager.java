package com.ec.application.indentpo;

import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.service.IndentStatusUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderLifecycleManager {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final POLineInwardSummaryService inwardSummaryService;
    private final IndentStatusUpdater indentStatusUpdater;

    @Transactional
    public void cancelIfAllowed(String poId) {

        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new IllegalStateException("PO not found"));

        if (inwardSummaryService.hasAnyInward(po.getLines())) {
            throw new IllegalStateException(
                    "PO has inward entries. It must be short closed."
            );
        }

        po.setStatus(POStatusConstants.STATUS_CANCELLED);
        purchaseOrderRepo.save(po);

        indentStatusUpdater.updateIndentStatuses(
                po.getLines(), POIndentUpdateAction.CANCEL_PO
        );
    }
}
