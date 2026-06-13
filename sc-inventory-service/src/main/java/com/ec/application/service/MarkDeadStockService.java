package com.ec.application.service;

import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.WarehouseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkDeadStockService {

    private final InventoryTransferService inventoryTransferService;
    private final StockCommentService stockCommentService;
    private final ProductRepo productRepo;
    private final WarehouseRepo warehouseRepo;

    public InventoryTransferResult markAsDeadStock(Long productId, MarkDeadStockRequest request) throws Exception {
        validate(request.getComment(), "Comment is mandatory when marking stock as dead");
        if (request.getQty() == null || request.getQty() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        String tenant = ThreadLocalStorage.getTenantName();
        if (tenant == null || tenant.trim().isEmpty()) {
            throw new IllegalStateException("Tenant context not available. Please select a project.");
        }

        // Warehouse is in project schema — must fetch while ThreadLocal = tenant
        Long deadStockWarehouseId = getDeadStockWarehouseId();

        // Product is in master schema — switch context explicitly
        String productName = getProductName(productId);

        CreateTransferDTO dto = buildTransferDTO(
                tenant, request.getSourceWarehouseId(), deadStockWarehouseId,
                productId, request.getQty(), request.getComment(), request.getOverrideBatches());

        InventoryTransferResult result = inventoryTransferService.createTransfer(dto);

        // createTransfer leaves ThreadLocal in an indeterminate state (withTenant finally sets to null).
        // Restore tenant before saving StockComment (which is in project schema).
        if (result.isFullySuccessful()) {
            ThreadLocalStorage.setTenantName(tenant);
            String commentIn = "Qty moved to Dead Stock: " + request.getQty() + ". " + request.getComment();
            stockCommentService.addSystemComment(productId, productName, commentIn,
                    "DEAD_STOCK_IN", result.getTransferId());
        }

        return result;
    }

    public InventoryTransferResult moveFromDeadStock(Long productId, MoveFromDeadStockRequest request) throws Exception {
        validate(request.getComment(), "Comment is mandatory when moving stock from dead stock");
        if (request.getQty() == null || request.getQty() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        String tenant = ThreadLocalStorage.getTenantName();
        if (tenant == null || tenant.trim().isEmpty()) {
            throw new IllegalStateException("Tenant context not available. Please select a project.");
        }

        Long deadStockWarehouseId = getDeadStockWarehouseId();
        String productName = getProductName(productId);

        CreateTransferDTO dto = buildTransferDTO(
                tenant, deadStockWarehouseId, request.getTargetWarehouseId(),
                productId, request.getQty(), request.getComment(), request.getOverrideBatches());

        InventoryTransferResult result = inventoryTransferService.createTransfer(dto);

        if (result.isFullySuccessful()) {
            ThreadLocalStorage.setTenantName(tenant);
            String commentOut = "Qty moved from Dead Stock: " + request.getQty() + ". " + request.getComment();
            stockCommentService.addSystemComment(productId, productName, commentOut,
                    "DEAD_STOCK_OUT", result.getTransferId());
        }

        return result;
    }

    private Long getDeadStockWarehouseId() {
        // Must be called with project tenant in ThreadLocal (warehouse is in project schema)
        return warehouseRepo.findByName(ProjectConstants.deadStockWarehouseName).stream()
                .findFirst()
                .map(w -> w.getWarehouseId())
                .orElseThrow(() -> new IllegalStateException("Dead Stock Warehouse not found for this project"));
    }

    private String getProductName(Long productId) {
        // Product is replicated to all schemas — current tenant schema is fine
        return productRepo.findById(productId)
                .map(p -> p.getProductName())
                .orElse("Product #" + productId);
    }

    private CreateTransferDTO buildTransferDTO(String tenant, Long sourceWarehouseId, Long targetWarehouseId,
                                                Long productId, Double qty, String remarks,
                                                List<BatchOverrideEntry> overrideBatches) {
        CreateTransferDTO dto = new CreateTransferDTO();
        dto.setTransferDate(new Date());
        dto.setSourceTenant(tenant);
        dto.setTargetTenant(tenant);
        dto.setSourceWarehouseId(sourceWarehouseId);
        dto.setTargetWarehouseId(targetWarehouseId);
        dto.setRemarks(remarks);

        InventoryTransferItemDTO item = new InventoryTransferItemDTO();
        item.setProductId(productId);
        item.setQuantity(qty);
        if (overrideBatches != null && !overrideBatches.isEmpty()) {
            item.setOverrideBatches(overrideBatches);
            item.setOverrideComment(remarks);
        }
        dto.setItems(Collections.singletonList(item));

        return dto;
    }

    private void validate(String comment, String message) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
