package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.BatchConsumptionPreviewDTO;
import com.ec.application.data.BatchUpdateRequestDTO;
import com.ec.application.data.OutwardBatchPreviewRequest;
import com.ec.application.data.StockSplitRequest;
import com.ec.application.data.StockTilesDTO;
import com.ec.application.data.WriteOffRequestDTO;
import com.ec.application.model.BatchWriteOff;
import com.ec.application.model.InventoryBatch;
import com.ec.application.model.OutwardBatchConsumption;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.OutwardBatchConsumptionRepository;
import com.ec.application.service.BatchTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class BatchTrackingController {

    @Autowired
    BatchTrackingService batchTrackingService;

    @Autowired
    OutwardBatchConsumptionRepository outwardBatchConsumptionRepository;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @PutMapping("/batch/{batchId}")
    public ResponseEntity<?> updateBatch(
            @PathVariable Long batchId,
            @RequestBody BatchUpdateRequestDTO request) {
        try {
            InventoryBatch result = batchTrackingService.updateBatch(batchId, request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/batch/{batchId}/write-off")
    public ResponseEntity<?> writeOffBatch(
            @PathVariable Long batchId,
            @RequestBody WriteOffRequestDTO request) {
        try {
            BatchWriteOff result = batchTrackingService.writeOffBatch(batchId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/batch/{batchId}/write-off/history")
    public ResponseEntity<List<BatchWriteOff>> getWriteOffHistory(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchTrackingService.getWriteOffHistory(batchId));
    }

    @GetMapping("/outward/{outwardId}/batch-consumptions")
    public ResponseEntity<List<OutwardBatchConsumption>> getBatchConsumptions(@PathVariable Long outwardId) {
        return ResponseEntity.ok(outwardBatchConsumptionRepository.findByOutwardIdOrderByIdAsc(outwardId));
    }

    @GetMapping("/inward/{inwardId}/batches")
    public ResponseEntity<List<InventoryBatch>> getBatchesForInward(@PathVariable Long inwardId) {
        return ResponseEntity.ok(inventoryBatchRepository.findAllByInwardId(inwardId));
    }

    @PostMapping("/outward/preview-batches")
    public ResponseEntity<?> previewBatchConsumption(@RequestBody OutwardBatchPreviewRequest request) {
        try {
            BatchConsumptionPreviewDTO result = batchTrackingService.previewBatchConsumption(
                    request.getProductId(), request.getWarehouseId(),
                    request.getQuantity(), request.getOverrideBatches());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/stock/{productId}/batches")
    public ResponseEntity<List<InventoryBatch>> getBatchesForProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(batchTrackingService.getBatchesForProduct(productId, warehouseId));
    }

    @PostMapping("/stock/tiles/expiry")
    public ResponseEntity<StockTilesDTO> getExpiryTiles(
            @RequestBody(required = false) FilterDataList filterDataList) {
        return ResponseEntity.ok(batchTrackingService.getStockTiles(filterDataList));
    }

    @PostMapping("/stock/{productId}/split-existing")
    public ResponseEntity<?> splitExistingStock(
            @PathVariable Long productId,
            @RequestBody StockSplitRequest request) {
        try {
            List<InventoryBatch> result = batchTrackingService.splitExistingStock(productId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }
}
