package com.ec.application.indentpo;

import com.ec.application.model.IndentsForInwardView;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.multitenant.ThreadLocalStorage;
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

    public boolean hasAnyInward(Set<PurchaseOrderLine> lines) {

        String tenant =
                tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());

        for (PurchaseOrderLine line : lines) {
            for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                List<IndentsForInwardView> rows =
                        inwardViewRepo.getLineItemForInward(ref.getIndentLineItemCode(), tenant);
                if (!rows.isEmpty()) return true;
            }
        }
        return false;
    }
}
