package com.ec.application.controller;

import com.ec.application.data.StockTilesDTO;
import com.ec.application.data.WriteOffRequestDTO;
import com.ec.application.model.BatchWriteOff;
import com.ec.application.model.InventoryBatch;
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

    @PostMapping("/batch/{batchId}/write-off")
    public ResponseEntity<?> writeOffBatch(
            @PathVariable Long batchId,
            @RequestBody WriteOffRequestDTO request) {
        try {
            BatchWriteOff result = batchTrackingService.writeOffBatch(batchId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/batch/{batchId}/write-off/history")
    public ResponseEntity<List<BatchWriteOff>> getWriteOffHistory(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchTrackingService.getWriteOffHistory(batchId));
    }

    @GetMapping("/stock/{productId}/batches")
    public ResponseEntity<List<InventoryBatch>> getBatchesForProduct(
            @PathVariable Long productId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(batchTrackingService.getBatchesForProduct(productId, warehouseId));
    }

    @GetMapping("/stock/tiles/expiry")
    public ResponseEntity<StockTilesDTO> getExpiryTiles() {
        return ResponseEntity.ok(batchTrackingService.getStockTiles());
    }
}
