package com.ec.application.enricher;

import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.service.LeadTimeResolver;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IndentInventoryUiEnricher {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    LeadTimeResolver leadTimeResolver;

    public void enrich(IndentInventory indent) {
        if (indent == null) return;

        // Approval flag
        try {
            boolean allowed = userDetailsService.canApproveRejectCancelIndent() && IndentStatusConstants.STATUS_NEW.equals(indent.getIndentStatus());
            indent.setApprovalAllowed(allowed);
        } catch (Exception e) {
            indent.setApprovalAllowed(false);
        }

        // Collect unique PO numbers from line items; also enrich lead time per line
        if (indent.getInventoryList() != null) {
            List<String> poNums = indent.getInventoryList().stream()
                    .filter(li -> li != null && !li.isDeleted() && li.getPurchaseOrderId() != null)
                    .map(IndentInventoryList::getPurchaseOrderId)
                    .distinct()
                    .collect(Collectors.toList());
            indent.setPoNumbers(poNums);

            for (IndentInventoryList item : indent.getInventoryList()) {
                if (item != null && item.getProduct() != null) {
                    item.setLeadTimeDays(leadTimeResolver.resolve(item.getProduct()));
                }
            }
        }

    }

    public void enrich(Collection<IndentInventory> indents) {
        if (indents == null) return;
        indents.forEach(this::enrich);
    }
}
