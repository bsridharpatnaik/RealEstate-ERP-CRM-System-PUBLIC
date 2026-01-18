package com.ec.application.indentpo;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.IndentsForInwardView;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentsForInwardViewRepository;
import com.ec.application.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class POLineInwardSummaryService {

    private final IndentsForInwardViewRepository inwardViewRepo;
    private final TenantService tenantService;
    private final IndentInventoryListRepo indentInventoryListRepo;

    public boolean hasAnyInward(Set<PurchaseOrderLine> lines) {
        String tenant = tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());
        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                List<IndentInventoryList> rows = indentInventoryListRepo.findByLineItemCodeAndStatuses(ref.getIndentLineItemCode(), IndentLineItemStatusConstants.INWARD_RECEIVED_STATUSES);
                        //inwardViewRepo.getLineItemForInward(ref.getIndentLineItemCode(), IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, tenant);
                if (!rows.isEmpty()) return true;
            }
        }
        return false;
    }

    public boolean isCancelledAllowed(Set<PurchaseOrderLine> lines) {
        String tenant = tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());
        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                List<IndentInventoryList> rows = indentInventoryListRepo.findByLineItemCodeAndStatuses(ref.getIndentLineItemCode(), IndentLineItemStatusConstants.INWARD_RECEIVED_STATUSES);
                //inwardViewRepo.getLineItemForInward(ref.getIndentLineItemCode(), IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, tenant);
                if (!rows.isEmpty()) return true;
            }
        }
        return false;
    }
}
