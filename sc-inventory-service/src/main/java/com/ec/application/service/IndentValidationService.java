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

        // Role check — same as before
        if (!canEditAsManager && !isStoreIncharge)
            throw new IllegalStateException("Indent cannot be updated in status: " + status + " by current user.");

        // Terminal statuses always block editing
        boolean isTerminal = IndentStatusConstants.getTerminalStatuses().stream()
                .anyMatch(s -> s.equalsIgnoreCase(status));
        if (isTerminal)
            throw new IllegalStateException("Indent cannot be edited in status: " + status);

        // Must have at least one NEW line item to allow edit
        boolean hasNewLineItem = indentInventory.getInventoryList().stream()
                .anyMatch(i -> !i.isDeleted()
                        && IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(i.getLineItemStatus()));
        if (!hasNewLineItem)
            throw new IllegalStateException("Indent cannot be edited — no editable line items. All items already have a PO or are closed.");
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
