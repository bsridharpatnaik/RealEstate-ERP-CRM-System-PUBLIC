package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FifoReportSyncService {

    private static final Logger log = LoggerFactory.getLogger(FifoReportSyncService.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy");

    private final OutwardBatchConsumptionRepository consumptionRepo;
    private final OutwardInventoryRepo outwardRepo;
    private final ProductRepo productRepo;
    private final GlobalFifoReportRepository globalFifoReportRepository;
    private final SchemaConfig schemaConfig;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting FIFO report sync for tenant: {}", tenantSchema);

        // Step 1: get last sync time from master
        ThreadLocalStorage.setTenantName(masterSchema);
        Date lastSyncTime = globalFifoReportRepository.findLastSyncTimeByTenantSchema(tenantSchema);
        boolean isFirstSync = (lastSyncTime == null);
        if (isFirstSync) {
            log.info("First sync for tenant {}. Loading all override records.", tenantSchema);
        } else {
            log.info("Incremental sync for tenant {}. Last sync time: {}", tenantSchema, lastSyncTime);
        }

        // Step 2: fetch changed override consumption rows from tenant schema
        ThreadLocalStorage.setTenantName(tenantSchema);
        List<OutwardBatchConsumption> consumptions = isFirstSync
                ? consumptionRepo.findAllOverrides()
                : consumptionRepo.findOverridesModifiedAfter(lastSyncTime);

        if (consumptions.isEmpty()) {
            log.info("No new FIFO override records for tenant: {}", tenantSchema);
            return;
        }

        log.info("Fetched {} override consumption rows for tenant: {}", consumptions.size(), tenantSchema);

        // Step 3: bulk fetch outward headers (still in tenant schema)
        List<Long> outwardIds = consumptions.stream()
                .map(OutwardBatchConsumption::getOutwardId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, OutwardInventory> outwardMap = outwardRepo.findByOutwardidIn(outwardIds)
                .stream()
                .collect(Collectors.toMap(OutwardInventory::getOutwardid, o -> o));

        // Step 4: get product names from master schema
        ThreadLocalStorage.setTenantName(masterSchema);
        List<Long> productIds = consumptions.stream()
                .map(OutwardBatchConsumption::getProductId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Product> productMap = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        // Step 5: upsert into master
        Date syncedAt = new Date();
        int inserted = 0, updated = 0, deleted = 0;

        ThreadLocalStorage.setTenantName(masterSchema);
        for (OutwardBatchConsumption c : consumptions) {
            InventoryBatch batch = c.getBatch();
            OutwardInventory outward = outwardMap.get(c.getOutwardId());
            Product product = productMap.get(c.getProductId());

            // If outward was deleted or not found, remove from master and skip
            if (outward == null || outward.isDeleted()) {
                globalFifoReportRepository.deleteByTenantSchemaAndOutwardIdAndBatchIdAndProductId(
                        tenantSchema, c.getOutwardId(),
                        batch != null ? batch.getBatchId() : 0L,
                        c.getProductId());
                deleted++;
                continue;
            }

            // If consumption itself was soft-deleted, remove from master and skip
            if (c.isDeleted()) {
                globalFifoReportRepository.deleteByTenantSchemaAndOutwardIdAndBatchIdAndProductId(
                        tenantSchema, c.getOutwardId(),
                        batch != null ? batch.getBatchId() : 0L,
                        c.getProductId());
                deleted++;
                continue;
            }

            Long batchId = batch != null ? batch.getBatchId() : 0L;

            Optional<GlobalFifoReport> existing = globalFifoReportRepository
                    .findByTenantSchemaAndOutwardIdAndBatchIdAndProductId(
                            tenantSchema, c.getOutwardId(), batchId, c.getProductId());

            GlobalFifoReport row = existing.orElseGet(GlobalFifoReport::new);
            boolean isNew = !existing.isPresent();

            row.setTenantSchema(tenantSchema);
            row.setOutwardId(c.getOutwardId());
            row.setOutwardDate(outward.getDate());
            row.setProductId(c.getProductId());
            row.setProductName(product != null ? product.getProductName() : "Unknown");
            row.setProductCode(product != null ? product.getProductCode() : null);
            row.setMeasurementUnit(product != null ? product.getMeasurementUnit() : null);
            row.setWarehouseId(c.getWarehouseId());
            row.setWarehouseName(outward.getWarehouse() != null ? outward.getWarehouse().getWarehouseName() : null);
            row.setUsageLocationName(outward.getUsageLocation() != null ? outward.getUsageLocation().getLocationName() : null);
            row.setUsageAreaName(outward.getUsageArea() != null ? outward.getUsageArea().getUsageAreaName() : null);
            row.setContractorName(outward.getContractor() != null ? outward.getContractor().getName() : null);
            row.setPurpose(outward.getPurpose());
            row.setBatchId(batchId);
            row.setBatchLotNumber(batch != null ? batch.getLotNumber() : null);
            row.setBatchBrand(batch != null ? batch.getBrand() : null);
            row.setBatchReceivedDate(batch != null && batch.getReceivedDate() != null
                    ? DATE_FMT.format(batch.getReceivedDate()) : null);
            row.setBatchExpiryDate(batch != null && batch.getExpiryDate() != null
                    ? DATE_FMT.format(batch.getExpiryDate()) : null);
            row.setQtyConsumed(c.getQtyConsumed());
            row.setOverrideComment(c.getOverrideComment());
            row.setPerformedBy(outward.getCreatedBy());
            row.setSyncedAt(syncedAt);

            globalFifoReportRepository.save(row);
            if (isNew) inserted++; else updated++;
        }

        log.info("FIFO report sync done for {}. Inserted={}, Updated={}, Deleted={}",
                tenantSchema, inserted, updated, deleted);

        // ── Step 6: Metadata refresh ─────────────────────────────────────────
        // Incremental sync only touches rows with recent lastModifiedDate.
        // If a product is renamed or its unit changes, existing rows keep stale data.
        // This pass re-reads Product metadata for every productId in the master table
        // and updates productName + measurementUnit regardless of lastModifiedDate.
        ThreadLocalStorage.setTenantName(masterSchema);
        List<Long> allSyncedProductIds =
                globalFifoReportRepository.findDistinctProductIdsByTenantSchema(tenantSchema);
        if (!allSyncedProductIds.isEmpty()) {
            Map<Long, Product> allProductMeta = productRepo.findAllById(allSyncedProductIds)
                    .stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));
            for (Long pid : allSyncedProductIds) {
                Product p = allProductMeta.get(pid);
                if (p != null) {
                    globalFifoReportRepository.updateProductMetadata(
                            tenantSchema, pid,
                            p.getProductName() != null ? p.getProductName() : "Unknown",
                            p.getMeasurementUnit());
                }
            }
            log.info("Metadata refresh done for {}. Products updated: {}", tenantSchema, allSyncedProductIds.size());
        }
    }
}
