package com.ec.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Getter
@Setter
public class DeadStockSyncStatus {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile String lastMessage;
    private volatile long lastStartTime;
    private volatile long lastEndTime;

    /**
     * Try to start the job.
     * @return true if started, false if already running
     */
    public boolean tryStart() {
        boolean started = running.compareAndSet(false, true);
        if (started) {
            lastStartTime = System.currentTimeMillis();
            lastMessage = "Running";
        }
        return started;
    }

    public void success() {
        running.set(false);
        lastEndTime = System.currentTimeMillis();
        lastMessage = "Completed successfully";
    }

    public void fail(Exception e) {
        running.set(false);
        lastEndTime = System.currentTimeMillis();
        lastMessage = "Failed: " + e.getMessage();
    }

    public boolean isRunning() {
        return running.get();
    }
}
