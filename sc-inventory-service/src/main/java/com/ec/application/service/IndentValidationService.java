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
        boolean canManage = userDetailsService.canApproveRejectCancelIndent();
        boolean isStoreIncharge = userDetailsService.isStoreIncharge();

        // Store Incharge can cancel only their own NEW (pre-approval) indents
        if (isStoreIncharge && IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status)) {
            return "CANCEL";
        }

        // Admin / Purchase Manager / Project Manager can reject NEW indents (before approval)
        if (canManage && IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status)) {
            return "REJECT";
        }

        // Admin / Purchase Manager / Project Manager can cancel an APPROVED indent if no POs created yet
        if (canManage && IndentStatusConstants.STATUS_APPROVED.equalsIgnoreCase(status)) {
            boolean hasPo = indentInventory.getInventoryList().stream()
                .anyMatch(item -> !IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(item.getLineItemStatus())
                               && !IndentLineItemStatusConstants.STATUS_SPLIT.equalsIgnoreCase(item.getLineItemStatus()));
            if (!hasPo) {
                return "CANCEL";
            }
            throw new IllegalStateException("Indent cannot be cancelled as one or more line items already have a PO created.");
        }

        throw new IllegalStateException("Indent cannot be cancelled or rejected in status: " + status + " by current user.");
    }

    public void validateBeforeApprove(IndentInventory indentInventory) throws Exception {
        String status = indentInventory.getIndentStatus();
        boolean canManage = userDetailsService.canApproveRejectCancelIndent();
        if (IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status) && canManage) {
            return;
        } else {
            throw new IllegalStateException("Indent cannot be approved in status: " + status + " by current user.");
        }
    }

    public void validateBeforeUpdate(IndentInventory indentInventory) throws Exception {
        String status = indentInventory.getIndentStatus();
        boolean canEditAsManager = userDetailsService.canEditIndentAsManager(); // admin, purchase-manager, project-manager
        boolean isStoreIncharge = userDetailsService.isStoreIncharge();

        // NEW indents: admin, purchase-manager, project-manager, or store-incharge can edit
        if (IndentStatusConstants.STATUS_NEW.equalsIgnoreCase(status) && (canEditAsManager || isStoreIncharge)) {
            return;
        }
        // APPROVED indents (before any PO is created): admin, purchase-manager, project-manager can edit
        // Store Incharge cannot edit once approved — they no longer own the record
        else if (IndentStatusConstants.STATUS_APPROVED.equalsIgnoreCase(status) && canEditAsManager) {
            return;
        }
        else {
            throw new IllegalStateException("Indent cannot be updated in status: " + status + " by current user.");
        }
    }

    public void validateBeforeSplit(IndentInventory indentInventory, IndentInventoryList lineItem) throws Exception {
        String indentStatus = indentInventory.getIndentStatus();
        String lineItemStatus = lineItem.getLineItemStatus();
        boolean isAdminOrPurchaseManager = userDetailsService.isAdminOrPurchaseManager();

        // Block on terminal indent statuses only
        boolean indentTerminal = IndentStatusConstants.getTerminalStatuses().stream()
                .anyMatch(s -> s.equalsIgnoreCase(indentStatus));
        boolean lineItemStatusAllowed = IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(lineItemStatus);

        if (!indentTerminal && isAdminOrPurchaseManager && lineItemStatusAllowed) {
            return;
        }

        if (indentTerminal) {
            throw new IllegalStateException(
                "Split not allowed: indent is in terminal status '" + indentStatus + "'.");
        }
        if (!isAdminOrPurchaseManager) {
            throw new IllegalStateException(
                "Split not allowed: current user does not have required role (Admin or Purchase Manager).");
        }
        throw new IllegalStateException(
            "Split not allowed: line item is in status '" + lineItemStatus + "'. Only NEW line items can be split.");
    }
}
