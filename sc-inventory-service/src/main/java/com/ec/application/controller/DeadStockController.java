package com.ec.application.controller;

import com.ec.application.config.SchemaConfig;
import com.ec.application.datasync.DeadStockSyncJob;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dead-stock")
public class DeadStockController {

    @Autowired
    private DeadStockSyncJob deadStockSyncJob;

    @PostMapping("/sync")
    public ResponseEntity<String> syncDeadStock() {

        if (deadStockSyncJob.isRunning()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Dead stock sync already running");
        }

        deadStockSyncJob.syncAllTenants();
        return ResponseEntity.ok("Dead stock sync completed");
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(
                deadStockSyncJob.isRunning() ? "RUNNING" : "IDLE"
        );
    }
}