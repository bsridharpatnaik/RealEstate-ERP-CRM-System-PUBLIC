package com.ec.application.controller;

import com.ec.application.config.SchemaConfig;
import com.ec.application.service.DeadStockSyncOrchestrator;
import com.ec.application.service.DeadStockSyncService;
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
    private DeadStockSyncOrchestrator deadStockSyncOrchestrator;

    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        deadStockSyncOrchestrator.syncAllTenants();
        return ResponseEntity.ok("Dead stock sync triggered");
    }
}