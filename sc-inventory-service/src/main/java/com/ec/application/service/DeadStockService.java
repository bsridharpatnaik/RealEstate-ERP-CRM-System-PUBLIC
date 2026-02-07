package com.ec.application.service;

import com.ec.application.Filters.StockSummarySpecification;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
import com.ec.application.model.StockSummary;
import com.ec.application.repository.StockSummaryRepo;
import com.ec.application.repository.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class DeadStockService {

    private final StockSummaryRepo stockSummaryRepo;
    private final PopulateDropdownService populateDropdownService;
    private final JobExecutionLogRepository jobExecutionLogRepo;

    Logger log = LoggerFactory.getLogger(DeadStockService.class);

    public StockSummary findSingleItem(Long id) {
        log.info("Invoked - findSingleItem | id={}", id);
        return stockSummaryRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Dead Stock Summary not found for id: " + id)
                );
    }


    public DeadStockWithDropdownData findFilteredDeadStock(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        DeadStockWithDropdownData returnData = new DeadStockWithDropdownData();
        Specification<StockSummary> spec = StockSummarySpecification.getSpecification(filterDataList);
        spec = StockSummarySpecification.addWarehouseForDeadStockFilter(spec);

        if (spec != null)
            returnData.setDaeadStockSummaries(stockSummaryRepo.findAll(spec, pageable));
        else
            returnData.setDaeadStockSummaries(stockSummaryRepo.findAll(pageable));

        returnData.setDeadStockDropdown(populateDropdownService.fetchData("deadstock"));
        returnData.setLastSyncDate(jobExecutionLogRepo.findLastSuccessfulRunTime("STOCK_SYNC"));
        return returnData;
    }

    public Map<Long, DeadStockDTOForIndent> fetchDeadStockForProductIds(List<Long> productIds) {

        Map<Long, DeadStockDTOForIndent> result = new HashMap<>();

        //Mandatory guard (prevents IN ())
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Initialize all productIds with zero values
        for (Long productId : productIds) {
            DeadStockDTOForIndent dto = new DeadStockDTOForIndent();
            dto.setToalDeadStock(0.0);
            dto.setDetailedDeadStock(new ArrayList<>());
            result.put(productId, dto);
        }

        //⃣ Fetch all matching dead stock rows
        List<StockSummary> summaries = stockSummaryRepo.fetchStockByProductIdsAndWarehouse(productIds, ProjectConstants.deadStockWarehouseName);

        if (summaries == null || summaries.isEmpty()) {
            return result; // all zero-filled
        }

        //️ Populate detailed + aggregate total
        for (StockSummary summary : summaries) {

            DeadStockDTOForIndent dto = result.get(summary.getProductId());
            if (dto == null) {
                continue; // defensive, should not happen
            }

            // detailedDeadStock entry: { tenantSchema : quantity }
            Map<String, Double> detail = new LinkedHashMap<>();
            detail.put(
                    summary.getTenantSchema(),
                    round2(summary.getQuantityInHand())
            );
            dto.getDetailedDeadStock().add(detail);

            // accumulate total
            dto.setToalDeadStock(
                    round2(dto.getToalDeadStock() + summary.getQuantityInHand())
            );
        }

        return result;
    }

    private double round2(Double value) {
        if (value == null) return 0.0;
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public DeadStockDTOForIndent fetchDeadStockForProductId(long productId) {
        List<Long> productIds = Collections.singletonList(productId);
        Map<Long, DeadStockDTOForIndent> deadStockMap = fetchDeadStockForProductIds(productIds);
        return deadStockMap.get(productId);
    }
}
