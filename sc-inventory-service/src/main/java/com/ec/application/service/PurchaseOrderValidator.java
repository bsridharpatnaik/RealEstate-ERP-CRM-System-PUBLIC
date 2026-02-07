package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.data.CreatePoLineRequest;
import com.ec.application.data.IndentLineRefRequest;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
}