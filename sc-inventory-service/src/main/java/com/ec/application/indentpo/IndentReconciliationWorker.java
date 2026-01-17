package com.ec.application.indentpo;

import com.ec.application.model.IndentInventoryList;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndentReconciliationWorker {

    private final IndentReconciliationTaskRepo repo;
    private final IndentLineItemStatusResolver statusResolver;
    private final IndentCompletionEvaluator completionEvaluator;
    private final IndentInventoryListRepo indentInventoryListRepo;

    @Scheduled(fixedDelay = 60000)
    public void run() {

        List<IndentReconciliationTask> tasks =
                repo.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        for (IndentReconciliationTask task : tasks) {

            try {
                ThreadLocalStorage.setTenantName(task.getTenant());

                statusResolver.resolve(task.getLineItemCode());

                IndentInventoryList item =
                        indentInventoryListRepo
                                .findByLineItemCode(task.getLineItemCode())
                                .get(0);

                completionEvaluator.evaluate(item.getIndentInventory());

                task.setStatus("DONE");

            } catch (Exception e) {
                task.setRetryCount(task.getRetryCount() + 1);
                task.setLastError(e.getMessage());
                task.setStatus(task.getRetryCount() > 5 ? "FAILED" : "PENDING");
            } finally {
                ThreadLocalStorage.setTenantName(null);
                repo.save(task);
            }
        }
    }
}
