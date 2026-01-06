package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.exception.InsufficientStockException;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.model.InventoryTransferItem;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryTransferRepository;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.WarehouseRepo;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryTransferService {

    @Autowired
    InventoryTransferRepository inventoryTransferRepository;

    @Autowired
    StockService stockService;

    Logger log = LoggerFactory.getLogger(InventoryTransferService.class);

    @Value("${master.schema}")
    private String masterSchema;

    @Autowired
    TenantService tenantService;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    WarehouseRepo warehouseRepo;

    public InventoryTransfer createTransfer(CreateTransferDTO dto) throws Exception {
        replaceTenantNamesForSuncity(dto);
        ThreadLocalStorage.setTenantName(dto.getSourceTenant());
        validateTransferRequest(dto);
        validateDuplicateProducts(dto);
        //validateSourceStockAvailability(dto);
        List<CurrentStockResponse> currentStocks = fetchCurrentStockInBulk(
                new BulkCurrentStockRequest(
                        dto.getSourceTenant(),
                        dto.getSourceWarehouseId(),
                        dto.getItems().stream().map(InventoryTransferItemDTO::getProductId).collect(Collectors.toList())
                )
        );
        InventoryTransfer transfer = new InventoryTransfer();
        Warehouse sourceWarehouse = warehouseRepo.findById(dto.getSourceWarehouseId()).orElseThrow(
                () -> new IllegalArgumentException("Source warehouse does not exist")
        );
        ThreadLocalStorage.setTenantName(dto.getTargetTenant());
        Warehouse targetWarehouse = warehouseRepo.findById(dto.getTargetWarehouseId()).orElseThrow(
                () -> new IllegalArgumentException("Target warehouse does not exist")
        );
        transfer.setSourceTenant(dto.getSourceTenant());
        transfer.setTargetTenant(dto.getTargetTenant());
        transfer.setSourceWarehouseName(sourceWarehouse.getWarehouseName());
        transfer.setTargetWarehouseName(targetWarehouse.getWarehouseName());
        transfer.setTransferDate(dto.getTransferDate());
        transfer.setTargetWarehouseId(targetWarehouse.getWarehouseId());
        transfer.setSourceWarehouseId(sourceWarehouse.getWarehouseId());
        List<InventoryTransferItem> items = new ArrayList<>();

        for (InventoryTransferItemDTO itemDTO : dto.getItems()) {
            InventoryTransferItem item = new InventoryTransferItem();
            Product product = productRepo.findByProductId(itemDTO.getProductId());
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setProductCode(product.getProductCode());
            item.setMeasurementUnit(product.getMeasurementUnit());
            item.setProductName(product.getProductName());
            item.setInventoryTransfer(transfer);
            items.add(item);
        }

        transfer.setItems(items);

        try {
            // SOURCE tenant → OUTWARD
            ThreadLocalStorage.setTenantName(dto.getSourceTenant());
            for (InventoryTransferItem item : items) {
                stockService.updateStock(
                        item.getProductId(),
                        transfer.getSourceWarehouseId(),
                        item.getQuantity(),
                        "outward"
                );
            }

            // TARGET tenant → INWARD
            ThreadLocalStorage.setTenantName(dto.getTargetTenant());
            for (InventoryTransferItem item : items) {
                stockService.updateStock(
                        item.getProductId(),
                        targetWarehouse.getWarehouseId(),
                        item.getQuantity(),
                        "inward"
                );
            }

            // MASTER → SAVE
            ThreadLocalStorage.setTenantName(masterSchema);
            return inventoryTransferRepository.save(transfer);

        } catch (Exception ex) {
            compensate(transfer, items);
            throw ex;
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    private void replaceTenantNamesForSuncity(CreateTransferDTO transfer) {
        transfer.setSourceTenant(tenantService.changeTenantForSuncity(transfer.getSourceTenant()));
        transfer.setTargetTenant(tenantService.changeTenantForSuncity(transfer.getTargetTenant()));
    }

    private void compensate(InventoryTransfer transfer, List<InventoryTransferItem> items) {

        try {
            ThreadLocalStorage.setTenantName(transfer.getTargetTenant());
            for (InventoryTransferItem item : items) {
                stockService.updateStock(item.getProductId(), transfer.getTargetWarehouseId(), item.getQuantity(), "outward");
            }

            ThreadLocalStorage.setTenantName(transfer.getSourceTenant());
            for (InventoryTransferItem item : items) {
                stockService.updateStock(item.getProductId(), transfer.getSourceWarehouseId(), item.getQuantity(), "inward");
            }

        } catch (Exception ex) {
            log.error("COMPENSATION FAILED", ex);
        }
    }

    private void validateTransferRequest(CreateTransferDTO transfer) {

        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }

        if (transfer.getSourceTenant() == null || transfer.getTargetTenant() == null) {
            throw new IllegalArgumentException("Source and Target tenant are mandatory");
        }

        if (transfer.getSourceWarehouseId() == null || transfer.getTargetWarehouseId() == null) {
            throw new IllegalArgumentException("Source and Target warehouse are mandatory");
        }

        if (!warehouseRepo.existsById(transfer.getSourceWarehouseId())) {
            throw new IllegalArgumentException("Source warehouse does not exist");
        }

        if (!warehouseRepo.existsById(transfer.getTargetWarehouseId())) {
            throw new IllegalArgumentException("Target warehouse does not exist");
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

        for (InventoryTransferItemDTO item : transfer.getItems()) {

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

    private void validateDuplicateProducts(CreateTransferDTO transfer) {
        // Key format: sourceTenant|sourceWarehouseId|productId
        Set<String> uniqueCombination = new HashSet<>();
        for (InventoryTransferItemDTO item : transfer.getItems()) {
            String key = transfer.getSourceTenant() + "|" + transfer.getSourceWarehouseId() + "|" + item.getProductId();
            if (!uniqueCombination.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate product in transfer for same source tenant and warehouse. Product ID: " + item.getProductId());
            }
        }
    }

    /*private void validateSourceStockAvailability(CreateTransferDTO transfer) throws Exception {
        ThreadLocalStorage.setTenantName(transfer.getSourceTenant());
        List<LowStockItem> lowStockItems = new ArrayList<>();
        try {
            List<Long> productIds = transfer.getItems()
                    .stream()
                    .map(InventoryTransferItemDTO::getProductId)
                    .collect(Collectors.toList());

            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(transfer.getSourceWarehouseId(), productIds);

            for (InventoryTransferItemDTO item : transfer.getItems()) {
                Double currentStock = stockMap.getOrDefault(item.getProductId(), 0.0);

                if (currentStock < item.getQuantity()) {
                    lowStockItems.add(
                            new LowStockItem(
                                    item.getProductId(),
                                    currentStock,
                                    item.getQuantity()
                            )
                    );
                }
            }

            if (!lowStockItems.isEmpty()) {
                throw new InsufficientStockException(lowStockItems);
            }

        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }*/


    public Page<InventoryTransfer> fetchTransfers(FilterDataList filterDataList, Pageable pageable) {
        return null;
    }

    public ResponseEntity<CurrentStockResponse> fetchCurrentStock(
            String tenant, Long productId, Long warehouseId) {

        try {
            ThreadLocalStorage.setTenantName(
                    tenantService.changeTenantForSuncity(tenant)
            );

            Double stock =
                    stockService.findStockForProductWarehouse(productId, warehouseId);

            return ResponseEntity.ok(
                    new CurrentStockResponse(
                            tenant,
                            productId,
                            warehouseId,
                            stock == null ? 0.0 : stock
                    )
            );

        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public List<CurrentStockResponse> fetchCurrentStockInBulk(BulkCurrentStockRequest request) throws Exception {
        try {
            //ThreadLocalStorage.setTenantName(tenantService.changeTenantForSuncity(request.getTenant()));
            ThreadLocalStorage.setTenantName(request.getTenant());
            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(request.getWarehouseId(), request.getProductIds());
            List<CurrentStockResponse> response = new ArrayList<>();

            for (Long productId : request.getProductIds()) {
                response.add(
                        new CurrentStockResponse(request.getTenant(), productId, request.getWarehouseId(), stockMap.getOrDefault(productId, 0.0)));
            }
            return response;
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}