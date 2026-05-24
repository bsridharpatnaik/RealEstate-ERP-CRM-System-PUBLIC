package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.CreatePoLineRequest;
import com.ec.application.data.IndentLineRefRequest;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderValidator {

    private final IndentInventoryListRepo indentInventoryListRepo;

    public void validateIndentLineItems(List<CreatePoLineRequest> lineItems) {

        List<String> invalidItems = new ArrayList<>();

        for (CreatePoLineRequest lineItem : lineItems) {
            for (IndentLineRefRequest indentRef : lineItem.getIndentRefs()) {
                List<IndentInventoryList> items = indentInventoryListRepo.findByLineItemCode(indentRef.getIndentLineItemCode());
                if (items.isEmpty())
                    throw new RuntimeException("Indent line item not found: " + indentRef.getIndentLineItemCode());
                IndentInventoryList ref = items.get(0);

                if (ref == null) {
                    invalidItems.add(indentRef.getIndentLineItemCode() + " (NOT FOUND)");
                } else if (!IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(ref.getLineItemStatus())) {

                    invalidItems.add(indentRef.getIndentLineItemCode() + " (Status: " + ref.getLineItemStatus() + ")");
                }
            }
        }

        if (!invalidItems.isEmpty()) {
            throw new RuntimeException(
                    "PO already exists for line items: " + String.join(", ", invalidItems)
            );
        }
    }

    private static final Set<String> NON_REMOVABLE_STATUSES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL,
            IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE,
            IndentLineItemStatusConstants.STATUS_SHORT_CLOSED,
            IndentLineItemStatusConstants.STATUS_CANCELLED
        ))
    );

    private static final Set<String> TERMINAL_PO_STATUSES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            POStatusConstants.STATUS_COMPLETED,
            POStatusConstants.STATUS_CANCELLED,
            POStatusConstants.STATUS_SHORT_CLOSED
        ))
    );

    /**
     * Validates that a PO line can be removed — all linked indent lines must be in PO CREATED status.
     */
    public void validateLineRemovable(PurchaseOrderLine line) {
        for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
            List<IndentInventoryList> items = indentInventoryListRepo.findByLineItemCode(ref.getIndentLineItemCode());
            if (items.isEmpty())
                throw new RuntimeException("Indent line item not found: " + ref.getIndentLineItemCode());
            IndentInventoryList item = items.get(0);
            if (NON_REMOVABLE_STATUSES.contains(item.getLineItemStatus())) {
                throw new RuntimeException(
                    "Cannot remove line — indent line item " + item.getLineItemCode() +
                    " is in status: " + item.getLineItemStatus() + ". Only lines in PO CREATED status can be removed.");
            }
        }
    }

    /**
     * Validates that the PO is in a state that allows adding new line items.
     */
    public void validateAddLineToPO(PurchaseOrder po) throws Exception {
        if (TERMINAL_PO_STATUSES.contains(po.getStatus())) {
            throw new Exception("Cannot add a line item to a PO in status: " + po.getStatus());
        }
    }

    public void validateOverridePhoneNumber(String overridePhoneNumber) throws Exception {
        if (overridePhoneNumber != null && overridePhoneNumber.length() > 35) {
            throw new Exception("Override Phone Number cannot exceed 35 characters.");
        }
    }
    public void validateOverrideEmail(String overrideEmail) throws Exception {
        if (overrideEmail != null && overrideEmail.length() > 100)
            throw new Exception("Override Email cannot exceed 100 characters.");
    }
}