package com.ec.application.service;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.RoleConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndentValidationService {

    @Autowired
    UserDetailsService userDetailsService;

    public String validateBeforeDelete(IndentInventory indentInventory) throws Exception {
        String status = indentInventory.getIndentStatus();
        boolean isAdminOrManager = userDetailsService.isAdminOrManager();
        boolean isInventoryExecutive = userDetailsService.isInventoryExecutive();

        // Executive can cancel only NEW (pre-approval) indents
        if (isInventoryExecutive && IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status)) {
            return "CANCEL";
        }

        // Admin/Manager can reject only NEW indents (before approval)
        if (isAdminOrManager && IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status)) {
            return "REJECT";
        }

        throw new IllegalStateException("Indent cannot be cancelled or rejected in status: " + status + " by current user.");
    }

    public void validateBeforeApprove(IndentInventory indentInventory) throws Exception {
        String status = indentInventory.getIndentStatus();
        boolean isAdminOrManager = userDetailsService.isAdminOrManager();
        if (IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status) && isAdminOrManager) {
            return;
        } else {
            throw new IllegalStateException("Indent cannot be approved in status: " + status + " by current user.");
        }
    }

    public void validateBeforeUpdate(IndentInventory indentInventory) throws Exception {
        String status = indentInventory.getIndentStatus();
        boolean isAdminOrManager = userDetailsService.isAdminOrManager();
        boolean isInventoryExecutive = userDetailsService.isInventoryExecutive();

        if (IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status) && isInventoryExecutive) {
            return;
        } else if (IndentStatusConstants.STATUS_APPROVED.equalsIgnoreCase(status) && isAdminOrManager) {
            return;
        } else {
            throw new IllegalStateException("Indent cannot be updated in status: " + status + "by current user.");
        }
    }

    public void validateBeforeSplit(IndentInventory indentInventory, IndentInventoryList lineItem) throws Exception {
        String indentStatus = indentInventory.getIndentStatus();
        String lineItemStatus = lineItem.getLineItemStatus();
        boolean isAdminOrManager = userDetailsService.isAdminOrManager();

        if (
                (indentStatus.equalsIgnoreCase(IndentStatusConstants.STATUS_APPROVED) || indentStatus.equalsIgnoreCase(IndentStatusConstants.STATUS_PO_PARTIAL)) &&
                 isAdminOrManager && IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(lineItemStatus)) {
            return;
        } else {
            throw new IllegalStateException("Indent line item cannot be split in status: " + lineItemStatus + " by current user.");
        }
    }
}
