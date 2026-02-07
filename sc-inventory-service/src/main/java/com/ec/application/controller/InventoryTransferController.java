package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.*;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.service.InventoryTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<InventoryTransferResult> createTransfer(@RequestBody CreateTransferDTO transfer) throws Exception {
        InventoryTransferResult savedTransfer = inventoryTransferService.createTransfer(transfer);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransfer);
    }

    @GetMapping("/current-stock")
    public ResponseEntity<CurrentStockResponse> getCurrentStock(@RequestParam String tenant, @RequestParam Long productId, @RequestParam Long warehouseId) {
        return inventoryTransferService.fetchCurrentStock(tenant, productId, warehouseId);
    }

    @PostMapping("/current-stock/bulk")
    public ResponseEntity<List<CurrentStockResponse>> fetchCurrentStockBulk(@RequestBody BulkCurrentStockRequest request) throws Exception {
        return ResponseEntity.ok(inventoryTransferService.fetchCurrentStockInBulk(request));
    }

    @GetMapping("/{id}")
    public InventoryTransfer findInventoryTransferById(@PathVariable long id) throws Exception {
        return inventoryTransferService.getOneInventoryTransfer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnInventoryTransferData fetchAllTransfers(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return inventoryTransferService.fetchTransfers(filterDataList, pageable);
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
    }
}