package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.InventoryTransferSpecification;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.exception.EntityNotFoundException;
import com.ec.application.exception.InsufficientStockException;
import com.ec.application.mapper.InventoryTransferMapper;
import com.ec.application.model.*;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryTransferService {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferService.class);

    private final InventoryTransferRepository inventoryTransferRepository;
    private final StockService stockService;
    private final TenantService tenantService;
    private final ProductRepo productRepo;
    private final WarehouseRepo warehouseRepo;
    private final InventoryTransferMapper inventoryTransferMapper;
    private final PopulateDropdownService populateDropdownService;

    @Value("${master.schema}")
    private String masterSchema;

    /* =====================================================
       MAIN API
       ===================================================== */

    public InventoryTransferResult createTransfer(CreateTransferDTO dto) throws Exception {
        replaceTenantNamesForSuncity(dto);
        validateTransferRequest(dto);
        validateDuplicateProducts(dto);
        validateSourceStockAvailability(dto);

        String sourceTenant = dto.getSourceTenant();
        String targetTenant = dto.getTargetTenant();

        Warehouse sourceWarehouse = withTenant(sourceTenant,
                () -> warehouseRepo.findById(dto.getSourceWarehouseId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Source warehouse not found")));

        Warehouse targetWarehouse = withTenant(targetTenant,
                () -> warehouseRepo.findById(dto.getTargetWarehouseId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Target warehouse not found")));

        Map<Long, Product> productMap = fetchProducts(dto, masterSchema);

        InventoryTransfer transfer = buildTransferEntity(dto, sourceWarehouse, targetWarehouse, productMap);
        List<TransferItemResult> itemResults = new ArrayList<>();
        List<InventoryTransferItem> successfulItems = new ArrayList<>();

        /* =====================================================
           PER-PRODUCT SAGA
           ===================================================== */
        for (InventoryTransferItem item : transfer.getItems()) {

            try {
                // DEBIT SOURCE
                Double sourceClosingStock = withTenant(sourceTenant, () ->
                        stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "outward"));

                try {
                    // CREDIT TARGET
                    Double targetClosingStock = withTenant(targetTenant, () ->
                            stockService.updateStock(item.getProductId(), targetWarehouse.getWarehouseId(), item.getQuantity(), "inward"));

                    // SET CLOSING STOCKS ONLY ON FULL SUCCESS
                    item.setSourceClosingStock(sourceClosingStock);
                    item.setTargetClosingStock(targetClosingStock);
                    successfulItems.add(item);
                    itemResults.add(new TransferItemResult(item.getProductId(), true, "Transfer successful"));
                } catch (Exception creditEx) {

                    // 3️⃣ COMPENSATE DEBIT
                    withTenant(sourceTenant, () -> {
                        stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "inward");
                        return null;
                    });

                    itemResults.add(new TransferItemResult(item.getProductId(), false, "Credit failed: " + creditEx.getMessage()));
                }

            } catch (Exception debitEx) {
                itemResults.add(new TransferItemResult(item.getProductId(), false, "Debit failed: " + debitEx.getMessage()));
            }
        }
        /* =====================================================
           PERSIST ONLY SUCCESSFUL ITEMS
           ===================================================== */
        transfer.setItems(successfulItems);
        if (!successfulItems.isEmpty()) {
            withTenant(masterSchema, () -> inventoryTransferRepository.save(transfer));
        }
        boolean fullySuccessful = itemResults.stream().allMatch(TransferItemResult::isSuccess);
        return new InventoryTransferResult(transfer.getTransferId(), fullySuccessful, itemResults);
    }

    /* =====================================================
       PRE-VALIDATION (UX ONLY)
       ===================================================== */

    /**
     * PRE-VALIDATION ONLY.
     * This improves UX by failing early.
     * Stock correctness is enforced during debit.
     */
    private void validateSourceStockAvailability(CreateTransferDTO transfer) throws Exception {
        withTenant(transfer.getSourceTenant(), () -> {
            List<Long> productIds = transfer.getItems()
                    .stream()
                    .map(InventoryTransferItemDTO::getProductId)
                    .collect(Collectors.toList());
            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(transfer.getSourceWarehouseId(), productIds, transfer.getSourceTenant());
            List<LowStockItem> lowStockItems = new ArrayList<>();
            for (InventoryTransferItemDTO item : transfer.getItems()) {
                double available = stockMap.getOrDefault(item.getProductId(), 0.0);

                if (available < item.getQuantity()) {
                    lowStockItems.add(new LowStockItem(item.getProductId(), available, item.getQuantity()));
                }
            }

            if (!lowStockItems.isEmpty()) {
                throw new InsufficientStockException(lowStockItems);
            }

            return null;
        });
    }

    /* =====================================================
       ENTITY BUILDING
       ===================================================== */

    private InventoryTransfer buildTransferEntity(
            CreateTransferDTO dto,
            Warehouse source,
            Warehouse target,
            Map<Long, Product> productMap) {

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setSourceTenant(dto.getSourceTenant());
        transfer.setTargetTenant(dto.getTargetTenant());
        transfer.setSourceWarehouseId(source.getWarehouseId());
        transfer.setTargetWarehouseId(target.getWarehouseId());
        transfer.setSourceWarehouseName(source.getWarehouseName());
        transfer.setTargetWarehouseName(target.getWarehouseName());
        transfer.setTransferDate(dto.getTransferDate());
        transfer.setRemarks(dto.getRemarks());
        List<InventoryTransferItem> items = dto.getItems().stream().map(i -> {
            Product p = productMap.get(i.getProductId());

            InventoryTransferItem item = new InventoryTransferItem();
            item.setProductId(p.getProductId());
            item.setProductCode(p.getProductCode());
            item.setProductName(p.getProductName());
            item.setMeasurementUnit(p.getMeasurementUnit());
            item.setQuantity(i.getQuantity());
            item.setInventoryTransfer(transfer);
            return item;
        }).collect(Collectors.toList());

        transfer.setItems(items);
        return transfer;
    }

    private Map<Long, Product> fetchProducts(CreateTransferDTO dto, String tenantName) {

        ThreadLocalStorage.setTenantName(tenantName);
        List<Long> productIds = dto.getItems()
                .stream()
                .map(InventoryTransferItemDTO::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepo.findByProductIdIn(productIds);

        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("One or more products not found");
        }

        return products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
    }

    /* =====================================================
       VALIDATIONS
       ===================================================== */

    private void validateTransferRequest(CreateTransferDTO transfer) {

        if (transfer == null)
            throw new IllegalArgumentException("Transfer request cannot be null");

        if (transfer.getItems() == null || transfer.getItems().isEmpty())
            throw new IllegalArgumentException("At least one item is required");

        if (transfer.getItems().size() > 5)
            throw new IllegalArgumentException("Maximum 5 items allowed");

        if (transfer.getSourceTenant().equalsIgnoreCase(transfer.getTargetTenant()) && transfer.getSourceWarehouseId().equals(transfer.getTargetWarehouseId())) {
            throw new IllegalArgumentException("Source and target warehouse must be different");
        }

        for (InventoryTransferItemDTO item : transfer.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product " + item.getProductId());
            }
        }
    }

    private void validateDuplicateProducts(CreateTransferDTO transfer) {

        Set<Long> productIds = new HashSet<>();
        for (InventoryTransferItemDTO item : transfer.getItems()) {
            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException("Duplicate product in transfer: " + item.getProductId());
            }
        }
    }

    private void replaceTenantNamesForSuncity(CreateTransferDTO transfer) {
        transfer.setSourceTenant(transfer.getSourceTenant());
        transfer.setTargetTenant(transfer.getTargetTenant());
    }

    /* =====================================================
       TENANT HELPER
       ===================================================== */

    private <T> T withTenant(String tenant, Callable<T> action) throws Exception {
        try {
            ThreadLocalStorage.setTenantName(tenant);
            return action.call();
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public ReturnInventoryTransferData fetchTransfers(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        ReturnInventoryTransferData returnData = new ReturnInventoryTransferData();
        Specification<InventoryTransfer> spec = InventoryTransferSpecification.getSpecification(filterDataList);
        Page<InventoryTransfer> page = (spec != null) ? inventoryTransferRepository.findAll(spec, pageable) : inventoryTransferRepository.findAll(pageable);
        returnData.setInventoryTransfers(page);
        returnData.setItDropdown(populateDropdownService.fetchData("inventorytransfer"));
        returnData.setMaxAllowedInventory(5);
        return returnData;
    }

    public ResponseEntity<CurrentStockResponse> fetchCurrentStock(
            String tenant, Long productId, Long warehouseId) {

        try {
            ThreadLocalStorage.setTenantName(tenant);

            Double stock = stockService.findStockForProductWarehouse(productId, warehouseId);
            return ResponseEntity.ok(new CurrentStockResponse(tenant, productId, warehouseId, stock == null ? 0.0 : stock));
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public List<CurrentStockResponse> fetchCurrentStockInBulk(BulkCurrentStockRequest request) throws Exception {
        try {
            String sourceTenant = request.getTenant();
            ThreadLocalStorage.setTenantName(sourceTenant);
            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(request.getWarehouseId(), request.getProductIds(), sourceTenant);
            List<CurrentStockResponse> response = new ArrayList<>();

            for (Long productId : request.getProductIds()) {
                response.add(new CurrentStockResponse(request.getTenant(), productId, request.getWarehouseId(), stockMap.getOrDefault(productId, 0.0)));
            }
            return response;
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public InventoryTransfer getOneInventoryTransfer(Long transferId) {
        return inventoryTransferRepository.findById(transferId).orElseThrow(() -> new IllegalArgumentException("Inventory Transfer not found"));
    }
}
