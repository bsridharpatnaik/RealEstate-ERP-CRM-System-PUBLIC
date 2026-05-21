package com.ec.application.indentpo;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.service.IndentStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IndentCompletionEvaluator {

    private final IndentInventoryRepo indentInventoryRepo;
    private final IndentStatusHistoryService indentStatusHistoryService;

    @Transactional
    public void evaluate(IndentInventory indent) {

        String oldStatus = indent.getIndentStatus();
        boolean allComplete = indent.getInventoryList().stream().allMatch(li ->
                Arrays.asList(
                        IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE,
                        IndentLineItemStatusConstants.STATUS_SHORT_CLOSED,
                        IndentLineItemStatusConstants.STATUS_CANCELLED
                ).contains(li.getLineItemStatus())
        );

        boolean anyInward = indent.getInventoryList().stream().anyMatch(li ->
                IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE.equals(li.getLineItemStatus())
                || IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL.equals(li.getLineItemStatus())
                || (IndentLineItemStatusConstants.STATUS_SHORT_CLOSED.equals(li.getLineItemStatus())
                        && li.getQuantityReceived() != null && li.getQuantityReceived() > 0)
        );

        boolean poComplete = indent.getInventoryList().stream().allMatch(li ->
                Arrays.asList(
                        IndentLineItemStatusConstants.STATUS_PO_CREATED,
                        IndentLineItemStatusConstants.STATUS_SHORT_CLOSED,
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
        if (!oldStatus.equals(indent.getIndentStatus())) {
            indentStatusHistoryService.logStatusChange(indent, oldStatus, indent.getIndentStatus(), "System", " Status changed from " + oldStatus + " to " + indent.getIndentStatus() + " - Auto-updated indent status based on line item statuses", null);
            indent.setLastStatusUpdatedAt(new Date());
        }
        indentInventoryRepo.save(indent);
    }
}
