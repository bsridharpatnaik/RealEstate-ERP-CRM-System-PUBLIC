package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.DeadStockWithDropdownData;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.model.Product;
import com.ec.application.service.DeadStockService;
import com.ec.application.service.DeadStockSyncOrchestrator;
import com.ec.application.service.DeadStockSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dead-stock")
public class DeadStockController {

    @Autowired
    private DeadStockSyncOrchestrator deadStockSyncOrchestrator;

    @Autowired
    DeadStockService deadStockService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = deadStockSyncOrchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @GetMapping("/{id}")
    public DeadStockSummary findDeadStock(@PathVariable long id) throws Exception {
        return deadStockService.findSingleItem(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public DeadStockWithDropdownData returnFilteredProducts(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) throws ParseException {
        return deadStockService.findFilteredDeadStock(filterDataList, pageable);
    }
}