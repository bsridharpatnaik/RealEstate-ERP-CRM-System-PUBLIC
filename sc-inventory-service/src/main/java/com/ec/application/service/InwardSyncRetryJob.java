package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.IndentInwardDeltaDTO;
import com.ec.application.data.IndentInwardSyncDTO;
import com.ec.application.indentpo.IndentInventoryAsyncUpdater;
import com.ec.application.model.InwardSyncFailure;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InwardSyncFailureRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class InwardSyncRetryJob {

    Logger log = LoggerFactory.getLogger(InwardSyncRetryJob.class);

    private static final int MAX_RETRIES = 5;

    private final InwardSyncFailureRepo inwardSyncFailureRepo;
    private final IndentInventoryAsyncUpdater indentInventoryAsyncUpdater;
    private final ObjectMapper objectMapper;

    /**
     * Retry failed inward → indent/PO syncs.
     * Runs every 10 minutes.
     */
    @Scheduled(cron = "0 */10 * * * *")
    @UseDefaultTenant
    @Transactional(readOnly = true)
    public void retryFailedInwardSyncs() {
        try {
            List<InwardSyncFailure> failures = inwardSyncFailureRepo.findTop20ByStatusOrderByCreationDateAsc("PENDING");
            if (failures.isEmpty()) {
                return;
            }

            log.info("[INWARD-RETRY] Found {} pending inward sync failures", failures.size());

            for (InwardSyncFailure failure : failures) {
                try {
                    log.info("[INWARD-RETRY-START] failureId={} tenant={} inwardId={} action={} retryCount={}", failure.getId(), failure.getTenantSchema(), failure.getInwardId(), failure.getActionType(), failure.getRetryCount());
                    // ---------------------------------------------
                    // Rebuild DTO from stored JSON payload
                    // ---------------------------------------------
                    IndentInwardSyncDTO dto = objectMapper.readValue(failure.getPayloadJson(), IndentInwardSyncDTO.class);

                    // ---------------------------------------------
                    // Replay async command
                    // ---------------------------------------------
                    indentInventoryAsyncUpdater.updateIndentAfterInwardAsync(dto, "create or update");

                    // ---------------------------------------------
                    // Mark success
                    // ---------------------------------------------
                    if (!"SUCCESS".equals(failure.getStatus())) {
                        failure.setStatus("SUCCESS");
                    }
                    log.info("[INWARD-RETRY-SUCCESS] failureId={} inwardId={}", failure.getId(), failure.getInwardId());
                } catch (Exception ex) {
                    int nextRetryCount = failure.getRetryCount() + 1;
                    failure.setRetryCount(nextRetryCount);
                    failure.setLastError(ex.getMessage());
                    if (nextRetryCount >= MAX_RETRIES) {
                        failure.setStatus("FAILED");
                        log.error("[INWARD-RETRY-FAILED] failureId={} inwardId={} retriesExceeded", failure.getId(), failure.getInwardId(), ex);

                    } else {
                        log.warn("[INWARD-RETRY-RETRYING] failureId={} inwardId={} retryCount={}", failure.getId(), failure.getInwardId(), nextRetryCount, ex);
                    }
                }
                inwardSyncFailureRepo.save(failure);
            }
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
