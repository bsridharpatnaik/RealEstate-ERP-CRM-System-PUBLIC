package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.StockSummarySpecification;
import com.ec.application.aspects.UseDefaultTenant;
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

        // Map productId → Product
        Map<Long, Product> productMap = new HashMap<>();
        List<Long> productIds = new ArrayList<>();

        for (Product p : dashboardProducts) {
            productIds.add(p.getProductId());
            productMap.put(p.getProductId(), p);
        }

        // Single DB call
        List<Object[]> rows = stockSummaryRepo.fetchTenantWiseStockForProducts(productIds);

        // productId → DTO
        Map<Long, DashboardProductStockDTO> dtoMap = new HashMap<>();

        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            String tenantSchema = (String) row[1];
            Double qty = (Double) row[2];

            DashboardProductStockDTO dto = dtoMap.get(productId);
            if (dto == null) {
                Product p = productMap.get(productId);

                dto = new DashboardProductStockDTO();
                dto.setProductId(productId);
                dto.setProductCode(p.getProductCode());
                dto.setProductName(p.getProductName());
                dto.setMeasurementUnit(p.getMeasurementUnit());
                dto.setTotalStock(0.0);
                dto.setTenantWiseStock(new ArrayList<TenantStockDTO>());

                dtoMap.put(productId, dto);
            }

            dto.getTenantWiseStock().add(
                    new TenantStockDTO(tenantSchema, qty)
            );

            dto.setTotalStock(dto.getTotalStock() + qty);
        }

        // Handle products with ZERO stock (important)
        for (Product p : dashboardProducts) {
            if (!dtoMap.containsKey(p.getProductId())) {
                DashboardProductStockDTO dto = new DashboardProductStockDTO();
                dto.setProductId(p.getProductId());
                dto.setProductCode(p.getProductCode());
                dto.setProductName(p.getProductName());
                dto.setMeasurementUnit(p.getMeasurementUnit());
                dto.setTotalStock(0.0);
                dto.setTenantWiseStock(Collections.emptyList());
                dtoMap.put(p.getProductId(), dto);
            }
        }

        return new ArrayList<DashboardProductStockDTO>(dtoMap.values());
    }
}
