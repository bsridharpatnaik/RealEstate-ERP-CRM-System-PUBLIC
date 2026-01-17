package com.ec.application.indentpo;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.IndentsForInwardView;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentsForInwardViewRepository;
import com.ec.application.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndentLineItemStatusResolver {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentsForInwardViewRepository inwardViewRepo;
    private final TenantService tenantService;

    @Transactional
    public void resolve(String lineItemCode) {

        IndentInventoryList item =
                indentInventoryListRepo.findByLineItemCode(lineItemCode).get(0);

        String tenant =
                tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());

        List<IndentsForInwardView> rows =
                inwardViewRepo.getLineItemForInward(lineItemCode, tenant);

        double ordered = 0;
        double inwarded = 0;

        for (IndentsForInwardView r : rows) {
            ordered += r.getQuantity();
            inwarded += r.getTotalInwardQuantity();
        }

        if (inwarded <= 0) return;

        if (Double.compare(inwarded, ordered) >= 0) {
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE);
        } else {
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL);
        }

        indentInventoryListRepo.save(item);
    }
}
