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
                // 1️⃣ Mark IN_PROGRESS (prevents double processing)
                task.setStatus("IN_PROGRESS");
                repo.save(task);

                // 2️⃣ Set tenant explicitly
                ThreadLocalStorage.setTenantName(task.getTenantSchema());

                // 3️⃣ Resolve inward → line item status
                statusResolver.resolve(task.getLineItemCode());

                // 4️⃣ Recalculate indent
                IndentInventoryList item =
                        indentInventoryListRepo
                                .findByLineItemCode(task.getLineItemCode())
                                .get(0);

                completionEvaluator.evaluate(item.getIndentInventory());

                // 5️⃣ Mark DONE
                task.setStatus("DONE");

            } catch (Exception e) {

                task.setRetryCount(task.getRetryCount() + 1);
                task.setLastError(e.getMessage());

                if (task.getRetryCount() >= 5) {
                    task.setStatus("FAILED");
                } else {
                    task.setStatus("PENDING");
                }

            } finally {
                // 6️⃣ Always clear tenant
                ThreadLocalStorage.setTenantName(null);
                repo.save(task);
            }
        }
    }
}
