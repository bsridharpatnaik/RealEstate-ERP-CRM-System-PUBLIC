package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.CurrentStockResponse;
import com.ec.application.data.LowStockItem;
import com.ec.application.exception.InsufficientStockException;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.model.InventoryTransferItem;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryTransferRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class InventoryTransferService {

    @Autowired
    private final InventoryTransferRepository inventoryTransferRepository;

    @Autowired
    private final StockService stockService;

    Logger log = LoggerFactory.getLogger(InventoryTransferService.class);

    @Value("${master.schema}")
    private String masterSchema;

    @Autowired
    TenantService tenantService;

    public InventoryTransfer createTransfer(InventoryTransfer transfer) throws Exception {
        replaceTenantNamesForSuncity(transfer);
        validateTransferRequest(transfer);
        validateDuplicateProducts(transfer);
        validateSourceStockAvailability(transfer);
        List<InventoryTransferItem> processedItems = new ArrayList<>();

        try {
            // SOURCE tenant - OUTWARD
            ThreadLocalStorage.setTenantName(transfer.getSourceTenant());
            for (InventoryTransferItem item : transfer.getItems()) {
                stockService.updateStock(item.getProductId(), transfer.getSourceWarehouseName(), item.getQuantity(), "outward");
                processedItems.add(item);
            }

            // TARGET tenant - INWARD
            ThreadLocalStorage.setTenantName(transfer.getTargetTenant());

            for (InventoryTransferItem item : processedItems) {
                stockService.updateStock(item.getProductId(), transfer.getTargetWarehouseName(), item.getQuantity(), "inward"
                );
            }

            // MASTER schema - SAVE TRANSFER
            ThreadLocalStorage.setTenantName(masterSchema);
            transfer.setTransferDate(new Date());
            transfer.getItems().forEach(i -> i.setInventoryTransfer(transfer));
            return inventoryTransferRepository.save(transfer);

        } catch (Exception ex) {

            // COMPENSATION LOGIC
            compensate(transfer, processedItems);
            throw ex;

        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    private void replaceTenantNamesForSuncity(InventoryTransfer transfer) {
        transfer.setSourceTenant(tenantService.changeTenantForSuncity(transfer.getSourceTenant()));
        transfer.setTargetTenant(tenantService.changeTenantForSuncity(transfer.getTargetTenant()));
    }

    private void compensate(InventoryTransfer transfer, List<InventoryTransferItem> processedItems) {
        try {
            // Revert TARGET inward (if already done)
            ThreadLocalStorage.setTenantName(transfer.getTargetTenant());

            for (InventoryTransferItem item : processedItems) {
                stockService.updateStock(item.getProductId(), transfer.getTargetWarehouseName(), item.getQuantity(), "outward");
            }

            // Revert SOURCE outward
            ThreadLocalStorage.setTenantName(transfer.getSourceTenant());
            for (InventoryTransferItem item : processedItems) {
                stockService.updateStock(item.getProductId(), transfer.getSourceWarehouseName(), item.getQuantity(), "inward");
            }
        } catch (Exception compensationEx) {
            // log & alert, do NOT swallow
            log.error("COMPENSATION FAILED for transfer", compensationEx);
        }
    }

    private void validateTransferRequest(InventoryTransfer transfer) {

        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }

        if (transfer.getSourceTenant() == null || transfer.getTargetTenant() == null) {
            throw new IllegalArgumentException("Source and Target tenant are mandatory");
        }

        if (transfer.getSourceWarehouseId() == null || transfer.getTargetWarehouseId() == null) {
            throw new IllegalArgumentException("Source and Target warehouse are mandatory");
        }

        if (transfer.getItems() == null || transfer.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required for transfer");
        }

        if (transfer.getItems().size() > 5) {
            throw new IllegalArgumentException("Maximum 5 items allowed per transfer");
        }

        // Same tenant + same warehouse is invalid
        if (transfer.getSourceTenant().equalsIgnoreCase(transfer.getTargetTenant())
                && transfer.getSourceWarehouseId().equals(transfer.getTargetWarehouseId())) {
            throw new IllegalArgumentException("Source and Target warehouse must be different");
        }

        for (InventoryTransferItem item : transfer.getItems()) {

            if (item.getProductId() == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero for product ID: " + item.getProductId()
                );
            }
        }
    }

    private void validateDuplicateProducts(InventoryTransfer transfer) {
        // Key format: sourceTenant|sourceWarehouseId|productId
        Set<String> uniqueCombination = new HashSet<>();
        for (InventoryTransferItem item : transfer.getItems()) {
            String key = transfer.getSourceTenant() + "|" + transfer.getSourceWarehouseId() + "|" + item.getProductId();
            if (!uniqueCombination.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate product in transfer for same source tenant and warehouse. Product ID: " + item.getProductId());
            }
        }
    }

    private void validateSourceStockAvailability(InventoryTransfer transfer) {
        List<LowStockItem> lowStockItems = new ArrayList<>();
        ThreadLocalStorage.setTenantName(transfer.getSourceTenant());
        try {
            for (InventoryTransferItem item : transfer.getItems()) {
                Double currentStock = stockService.findStockForProductWarehouse(item.getProductId(), transfer.getSourceWarehouseId());
                if (currentStock == null || currentStock <= 0 || currentStock < item.getQuantity()) {
                    lowStockItems.add(new LowStockItem(item.getProductId(), currentStock == null ? 0.0 : currentStock, item.getQuantity()));
                }
            }
            if (!lowStockItems.isEmpty()) {
                throw new InsufficientStockException(lowStockItems);
            }
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public Page<InventoryTransfer> fetchTransfers(FilterDataList filterDataList, Pageable pageable) {
        return null;
    }

    public ResponseEntity<CurrentStockResponse> fetchCurrentStock(String tenant, Long productId, Long warehouseId) {
        try {
            ThreadLocalStorage.setTenantName(tenantService.changeTenantForSuncity(tenant));
            Double currentStock = stockService.findStockForProductWarehouse(productId, warehouseId);
            return ResponseEntity.ok(new CurrentStockResponse(tenant, productId, warehouseId, currentStock == null ? 0.0 : currentStock));
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}