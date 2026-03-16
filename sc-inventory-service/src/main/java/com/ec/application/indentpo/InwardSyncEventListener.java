package com.ec.application.indentpo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InwardSyncEventListener {

    private static final Logger log = LoggerFactory.getLogger(InwardSyncEventListener.class);

    private final IndentInventoryAsyncUpdater asyncUpdater;

    public InwardSyncEventListener(IndentInventoryAsyncUpdater asyncUpdater) {
        this.asyncUpdater = asyncUpdater;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInwardCommitted(InwardSyncEvent event) throws JsonProcessingException {
        log.info("[EVENT] Inward committed, triggering async sync action={}", event.getAction());
        asyncUpdater.updateIndentAfterInwardAsync(event.getDto(), event.getAction());
    }
}