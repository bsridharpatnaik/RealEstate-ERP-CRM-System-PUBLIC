package com.ec.application.enricher;

import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.service.UserDetailsService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class IndentInventoryUiEnricher {

    @Autowired
    UserDetailsService userDetailsService;

    public void enrich(IndentInventory indent) {
        if (indent == null) return;

        try {
            boolean allowed = userDetailsService.isAdminOrManager() && IndentStatusConstants.STATUS_NEW.equals(indent.getIndentStatus());
            indent.setApprovalAllowed(allowed);
        } catch (Exception e) {
            // fail-safe: UI flag should never break response
            indent.setApprovalAllowed(false);
        }
    }

    public void enrich(Collection<IndentInventory> indents) {
        if (indents == null) return;
        indents.forEach(this::enrich);
    }
}
