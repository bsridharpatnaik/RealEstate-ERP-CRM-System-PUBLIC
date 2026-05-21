package com.ec.application.indentpo;

import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IndentStatusBackfillService {

    private final IndentInventoryRepo indentInventoryRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final SchemaConfig schemaConfig;

    private static final Logger log = LoggerFactory.getLogger(IndentStatusBackfillService.class);

    private volatile boolean running = false;
    private volatile boolean completed = false;
    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicInteger fixed = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    private volatile String startedAt = null;
    private volatile String completedAt = null;
    private volatile String lastError = null;

    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("running", running);
        s.put("completed", completed);
        s.put("total", total.get());
        s.put("fixed", fixed.get());
        s.put("skipped", skipped.get());
        s.put("errors", errors.get());
        s.put("pending", total.get() - fixed.get() - skipped.get() - errors.get());
        s.put("startedAt", startedAt);
        if (completedAt != null) s.put("completedAt", completedAt);
        if (lastError != null) s.put("lastError", lastError);
        return s;
    }

    public Map<String, Object> start() {
        if (running) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("message", "Backfill already in progress");
            r.putAll(getStatus());
            return r;
        }
        running = true;
        completed = false;
        total.set(0);
        fixed.set(0);
        skipped.set(0);
        errors.set(0);
        startedAt = LocalDateTime.now().toString();
        completedAt = null;
        lastError = null;

        new Thread(this::run, "indent-status-backfill").start();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("message", "Backfill started in background");
        r.put("startedAt", startedAt);
        r.put("tip", "Poll GET /master-file/admin/indent-status-backfill/status for progress");
        return r;
    }

    private void run() {
        try {
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());

            List<String> targetStatuses = Arrays.asList(
                    IndentStatusConstants.STATUS_APPROVED,
                    IndentStatusConstants.STATUS_PO_PARTIAL,
                    IndentStatusConstants.STATUS_PO_COMPLETED,
                    IndentStatusConstants.STATUS_INWARD_PARTIAL
            );

            List<IndentInventory> candidates = indentInventoryRepo.findByIndentStatusIn(targetStatuses);
            total.set(candidates.size());
            log.info("[BACKFILL-START] Found {} candidate indents", candidates.size());

            for (IndentInventory candidate : candidates) {
                try {
                    String beforeStatus = candidate.getIndentStatus();
                    Optional<IndentInventory> full = indentInventoryRepo.findByIdWithDetails(candidate.getIndentId());
                    if (!full.isPresent()) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    indentCompletionEvaluator.evaluate(full.get());
                    String afterStatus = full.get().getIndentStatus();
                    if (!beforeStatus.equals(afterStatus)) {
                        log.info("[BACKFILL-FIX] indent={} {} → {}", candidate.getIndentId(), beforeStatus, afterStatus);
                        fixed.incrementAndGet();
                    } else {
                        skipped.incrementAndGet();
                    }
                } catch (Exception e) {
                    lastError = candidate.getIndentId() + ": " + e.getMessage();
                    log.error("[BACKFILL-ERROR] indent={}: {}", candidate.getIndentId(), e.getMessage());
                    errors.incrementAndGet();
                }
            }
        } catch (Exception e) {
            log.error("[BACKFILL-FATAL] {}", e.getMessage(), e);
            lastError = e.getMessage();
        } finally {
            ThreadLocalStorage.setTenantName(null);
            running = false;
            completed = true;
            completedAt = LocalDateTime.now().toString();
            log.info("[BACKFILL-DONE] total={}, fixed={}, skipped={}, errors={}",
                    total.get(), fixed.get(), skipped.get(), errors.get());
        }
    }
}
