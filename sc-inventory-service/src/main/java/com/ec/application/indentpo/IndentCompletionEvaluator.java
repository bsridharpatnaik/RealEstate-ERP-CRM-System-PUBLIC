package com.ec.application.indentpo;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.repository.IndentInventoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IndentCompletionEvaluator {

    private final IndentInventoryRepo indentInventoryRepo;

    @Transactional
    public void evaluate(IndentInventory indent) {

        boolean allComplete = indent.getInventoryList().stream().allMatch(li ->
                Arrays.asList(
                        IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE,
                        IndentLineItemStatusConstants.STATUS_SHORT_CLOSED,
                        IndentLineItemStatusConstants.STATUS_CANCELLED
                ).contains(li.getLineItemStatus())
        );

        boolean anyInward = indent.getInventoryList().stream().anyMatch(li ->
                IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL.equals(li.getLineItemStatus())
        );

        boolean poComplete = indent.getInventoryList().stream().allMatch(li ->
                Arrays.asList(
                        IndentLineItemStatusConstants.STATUS_PO_CREATED,
                        IndentLineItemStatusConstants.STATUS_CANCELLED
                ).contains(li.getLineItemStatus())
        );

        boolean anyPoCreated = indent.getInventoryList().stream().anyMatch(li ->
                IndentLineItemStatusConstants.STATUS_PO_CREATED.equals(li.getLineItemStatus())
        );

        if (allComplete) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_CLOSED);
        } else if (anyInward) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_INWARD_PARTIAL);
        } else if (poComplete) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_PO_COMPLETED);
        } else if (anyPoCreated) {
            indent.setIndentStatus(IndentStatusConstants.STATUS_PO_PARTIAL);
        } else {
            indent.setIndentStatus(IndentStatusConstants.STATUS_APPROVED);
        }
        indentInventoryRepo.save(indent);
    }
}
