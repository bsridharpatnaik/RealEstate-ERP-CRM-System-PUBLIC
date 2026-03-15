package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.StockSummarySpecification;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
import com.ec.application.model.Product;
import com.ec.application.model.StockSummary;
import com.ec.application.repository.JobExecutionLogRepository;
import com.ec.application.repository.StockSummaryRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class StockSummaryService {

    private final StockSummaryRepo stockSummaryRepo;
    private final PopulateDropdownService populateDropdownService;
    private final JobExecutionLogRepository jobExecutionLogRepo;
    private final ProductService productService;
    private final SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(StockSummaryService.class);

    public StockSummaryWithDropdownData findFilteredStockSummary(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        StockSummaryWithDropdownData returnData = new StockSummaryWithDropdownData();
        Page<StockSummaryAggregatedDTO> data = stockSummaryRepo.fetchAggregatedStock(filterDataList, pageable);
        returnData.setStockSummaries(data);
        returnData.setStockDropdown(populateDropdownService.fetchData("deadstock"));
        returnData.setLastSyncDate(jobExecutionLogRepo.findLastSuccessfulRunTime("STOCK_SYNC"));
        return returnData;
    }

    @Transactional(readOnly = true)
    public List<DashboardProductStockDTO> getDashboardProductStock() {

        List<Product> dashboardProducts = productService.getDashboardProducts();
        if (dashboardProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // All tenant names from config (excluding master)
        List<String> allTenants = schemaConfig.getNonMasterSchemaList();

        Map<Long, Product> productMap = new HashMap<>();
        List<Long> productIds = new ArrayList<>();
        for (Product p : dashboardProducts) {
            productIds.add(p.getProductId());
            productMap.put(p.getProductId(), p);
        }

        // productId → DTO
        Map<Long, DashboardProductStockDTO> dtoMap = new HashMap<>();
        // productId → tenantSchema → TenantStockDTO (for merging dead stock in)
        Map<Long, Map<String, TenantStockDTO>> tenantMap = new HashMap<>();

        // ── Pre-initialise every product with ALL tenants at zero ──────────
        for (Product p : dashboardProducts) {
            DashboardProductStockDTO dto = new DashboardProductStockDTO();
            dto.setProductId(p.getProductId());
            dto.setProductCode(p.getProductCode());
            dto.setProductName(p.getProductName());
            dto.setMeasurementUnit(p.getMeasurementUnit());
            dto.setTotalStock(0.0);
            dto.setTotalDeadStock(0.0);
            dto.setTenantWiseStock(new ArrayList<>());
            dtoMap.put(p.getProductId(), dto);

            Map<String, TenantStockDTO> tenantEntries = new LinkedHashMap<>();
            for (String tenant : allTenants) {
                TenantStockDTO tenantStock = new TenantStockDTO(tenant, 0.0, 0.0);
                dto.getTenantWiseStock().add(tenantStock);
                tenantEntries.put(tenant, tenantStock);
            }
            tenantMap.put(p.getProductId(), tenantEntries);
        }

        // ── Regular stock — update matching tenant entries ─────────────────
        List<Object[]> rows = stockSummaryRepo.fetchTenantWiseStockForProducts(productIds);
        for (Object[] row : rows) {
            Long productId = (Long)   row[0];
            String tenant  = (String) row[1];
            Double qty     = (Double) row[2];

            DashboardProductStockDTO dto = dtoMap.get(productId);
            if (dto == null) continue;

            TenantStockDTO tenantStock = tenantMap.get(productId).get(tenant);
            if (tenantStock != null) {
                tenantStock.setQuantity(qty);
                dto.setTotalStock(dto.getTotalStock() + qty);
            }
        }

        // ── Dead stock — update matching tenant entries ────────────────────
        List<Object[]> deadRows = stockSummaryRepo.fetchTenantWiseDeadStockForProducts(
                productIds, ProjectConstants.deadStockWarehouseName
        );
        for (Object[] row : deadRows) {
            Long productId = (Long)   row[0];
            String tenant  = (String) row[1];
            Double deadQty = (Double) row[2];

            DashboardProductStockDTO dto = dtoMap.get(productId);
            if (dto == null) continue;

            TenantStockDTO tenantStock = tenantMap.get(productId).get(tenant);
            if (tenantStock != null) {
                tenantStock.setDeadStock(deadQty);
                dto.setTotalDeadStock(dto.getTotalDeadStock() + deadQty);
            }
        }

        return new ArrayList<>(dtoMap.values());
    }
}
