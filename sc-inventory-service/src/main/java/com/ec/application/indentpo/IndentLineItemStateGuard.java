package com.ec.application.indentpo;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.model.IndentInventoryList;
import org.springframework.stereotype.Service;

@Service
public class IndentLineItemStateGuard {

    public void assertEditable(IndentInventoryList item) {
        if (!IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(item.getLineItemStatus())) {
            throw new IllegalStateException(
                    "Line item " + item.getLineItemCode() + " cannot be edited in status " + item.getLineItemStatus()
            );
        }
    }

    public void assertSplittable(IndentInventoryList item) {
        assertEditable(item);
        if (item.getPurchaseOrderId() != null) {
            throw new IllegalStateException(
                    "Line item " + item.getLineItemCode() + " is already linked to a PO"
            );
        }
    }

    public void assertPoAttachable(IndentInventoryList item) {
        if (!IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(item.getLineItemStatus())) {
            throw new IllegalStateException(
                    "Line item " + item.getLineItemCode() + " is not eligible for PO"
            );
        }
    }

    public void assertInwardAllowed(IndentInventoryList item) {
        if (IndentLineItemStatusConstants.STATUS_SHORT_CLOSED.equalsIgnoreCase(item.getLineItemStatus())) {
            throw new IllegalStateException(
                    "Inward not allowed for short closed line item " + item.getLineItemCode()
            );
        }
    }
}
