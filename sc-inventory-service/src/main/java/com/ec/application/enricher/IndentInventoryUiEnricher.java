package com.ec.application.enricher;

import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

@Component
public class IndentInventoryUiEnricher {

    @Autowired
    UserDetailsService userDetailsService;

    public void enrich(IndentInventory indent) {
        if (indent == null) return;

        // Approval flag
        try {
            boolean allowed = userDetailsService.canApproveRejectCancelIndent() && IndentStatusConstants.STATUS_NEW.equals(indent.getIndentStatus());
            indent.setApprovalAllowed(allowed);
        } catch (Exception e) {
            indent.setApprovalAllowed(false);
        }

        // Compute effective needByDate = minimum across all active line item dates
        if (indent.getInventoryList() != null) {
            Date minDate = indent.getInventoryList().stream()
                    .filter(li -> li != null && !li.isDeleted() && li.getNeedByDate() != null)
                    .map(IndentInventoryList::getNeedByDate)
                    .min(Date::compareTo)
                    .orElse(null);
            indent.setNeedByDate(minDate);
        }
    }

    public void enrich(Collection<IndentInventory> indents) {
        if (indents == null) return;
        indents.forEach(this::enrich);
    }
}
