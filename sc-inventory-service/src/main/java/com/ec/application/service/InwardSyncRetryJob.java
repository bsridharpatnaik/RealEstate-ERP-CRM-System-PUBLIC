package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.indentpo.IndentInventoryAsyncUpdater;
import com.ec.application.model.InwardSyncFailure;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InwardSyncFailureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class InwardSyncRetryJob {

    private final InwardSyncFailureRepo failureRepo;
    private final IndentInventoryAsyncUpdater asyncUpdater;

    @Scheduled(cron = "0 */10 * * * *") // every 10 minutes
    @UseDefaultTenant
    public void retryFailedInwardSyncs() {
        try {

            List<InwardSyncFailure> failures = failureRepo.findTop20ByStatusOrderByCreatedDateAsc("PENDING");

            for (InwardSyncFailure failure : failures) {

                try {
                    Set<String> lineItemCodes =
                            new HashSet<>(
                                    Arrays.asList(
                                            failure.getLineItemCodes().split(",")
                                    )
                            );

                    asyncUpdater.updateIndentAfterInwardAsync(
                            failure.getTenantSchema(),
                            failure.getActionType(),
                            failure.getInwardId(),
                            lineItemCodes
                    );
                    failure.setStatus("SUCCESS");
                } catch (Exception ex) {
                    failure.setRetryCount(failure.getRetryCount() + 1);
                    failure.setLastError(ex.getMessage());
                    if (failure.getRetryCount() >= 5) {
                        failure.setStatus("FAILED");
                    }
                }
                failureRepo.save(failure);
            }
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
