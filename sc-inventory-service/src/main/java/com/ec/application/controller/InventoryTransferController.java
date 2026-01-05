package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.BulkCurrentStockRequest;
import com.ec.application.data.CreateTransferDTO;
import com.ec.application.data.CurrentStockResponse;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.InventoryTransferService;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/inventory-transfer")
public class InventoryTransferController {

    @Autowired
    InventoryTransferService inventoryTransferService;

    @Value("${master.schema}")
    private String masterSchema;

    // =========================
    // CREATE TRANSFER
    // =========================
    @PostMapping("/create")
    public ResponseEntity<InventoryTransfer> createTransfer(@RequestBody CreateTransferDTO transfer) throws Exception {
        InventoryTransfer savedTransfer = inventoryTransferService.createTransfer(transfer);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransfer);
    }

    // =========================
    // FETCH PAST TRANSFERS (FILTER + PAGINATION)
    // =========================
    @PostMapping
    public ResponseEntity<Page<InventoryTransfer>> fetchTransfers(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InventoryTransfer> transfers = inventoryTransferService.fetchTransfers(filterDataList, pageable);
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/current-stock")
    public ResponseEntity<CurrentStockResponse> getCurrentStock(@RequestParam String tenant, @RequestParam Long productId, @RequestParam Long warehouseId) {
        return inventoryTransferService.fetchCurrentStock(tenant, productId, warehouseId);
    }

    @PostMapping("/current-stock/bulk")
    public ResponseEntity<List<CurrentStockResponse>> fetchCurrentStockBulk(@RequestBody BulkCurrentStockRequest request) throws Exception {
        return ResponseEntity.ok(inventoryTransferService.fetchCurrentStockInBulk(request));
    }
}