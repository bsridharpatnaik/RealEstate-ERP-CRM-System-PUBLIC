package com.ec.application.enricher;

import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.service.LeadTimeResolver;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class PurchaseOrderUiEnricher {

    private static final List<String> TERMINAL_STATUSES = Arrays.asList(
            "CANCELLED", "COMPLETE INWARD", "SHORT CLOSED", "SHORT CLOSE"
    );

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    LeadTimeResolver leadTimeResolver;

    public void enrich(PurchaseOrder po) {
        if (po == null) return;

        try {
            po.setApprovalAllowed(true);
            po.setCancellationAllowed(true);
        } catch (Exception e) {
            po.setApprovalAllowed(false);
            po.setCancellationAllowed(false);
        }

        // Compute hasOverdueLines for list-level badge
        enrichOverdueFlag(po);
    }

    /**
     * Computes hasOverdueLines on the PO header.
     * Also enriches each line with leadTimeDays, daysLeft, isOverdue when lines are loaded.
     */
    public void enrichOverdueFlag(PurchaseOrder po) {
        if (po == null || po.getLines() == null || po.getLines().isEmpty()) return;

        boolean isTerminal = TERMINAL_STATUSES.stream()
                .anyMatch(s -> s.equalsIgnoreCase(po.getStatus()));

        if (isTerminal) {
            po.setHasOverdueLines(false);
            return;
        }

        LocalDate poDate = po.getPoDate() != null
                ? po.getPoDate().toInstant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
                : null;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        boolean anyOverdue = false;
        for (PurchaseOrderLine line : po.getLines()) {
            if (line.isDeleted()) continue;
            // Skip fully received lines
            if ("INWARD_COMPLETE".equalsIgnoreCase(line.getLineItemStatus())) continue;

            Integer lt = leadTimeResolver.resolve(line.getProduct());
            line.setLeadTimeDays(lt);

            if (lt != null && poDate != null) {
                long daysSincePO = ChronoUnit.DAYS.between(poDate, today);
                int daysLeft = lt - (int) daysSincePO;
                line.setDaysLeft(daysLeft);
                boolean overdue = daysLeft < 0;
                line.setIsOverdue(overdue);
                if (overdue) anyOverdue = true;
            } else {
                line.setDaysLeft(null);
                line.setIsOverdue(false);
            }
        }
        po.setHasOverdueLines(anyOverdue);
    }

    public void enrich(Collection<PurchaseOrder> pos) {
        if (pos == null) return;
        pos.forEach(this::enrich);
    }
}

