package com.ec.application.service;

import com.ec.application.Filters.CategorySpecifications;
import com.ec.application.Filters.DeadStockSpecification;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.model.Category;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.model.JobExecutionLog;
import com.ec.application.repository.DeadStockSummaryRepo;
import com.ec.application.repository.JobExecutionLogRepository;
import com.ec.application.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class DeadStockService {

    private final DeadStockSummaryRepo deadStockSummaryRepo;
    private final PopulateDropdownService populateDropdownService;
    private final JobExecutionLogRepository jobExecutionLogRepo;

    Logger log = LoggerFactory.getLogger(CategoryService.class);

    public DeadStockSummary findSingleItem(Long id) {
        log.info("Invoked - findSingleItem | id={}", id);
        return deadStockSummaryRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Dead Stock Summary not found for id: " + id)
                );
    }


    public DeadStockWithDropdownData findFilteredDeadStock(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        DeadStockWithDropdownData returnData = new DeadStockWithDropdownData();
        Specification<DeadStockSummary> spec = DeadStockSpecification.getSpecification(filterDataList);

        if (spec != null)
            returnData.setDaeadStockSummaries(deadStockSummaryRepo.findAll(spec, pageable));
        else
            returnData.setDaeadStockSummaries(deadStockSummaryRepo.findAll(pageable));

        returnData.setDeadStockDropdown(populateDropdownService.fetchData("deadstock"));
        returnData.setLastSyncDate(jobExecutionLogRepo.findLastSuccessfulRunTime("DEAD_STOCK_SYNC"));
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
        List<DeadStockSummary> summaries = deadStockSummaryRepo.fetchDeadStockByProductIds(productIds);

        if (summaries == null || summaries.isEmpty()) {
            return result; // all zero-filled
        }

        //️ Populate detailed + aggregate total
        for (DeadStockSummary summary : summaries) {

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

}
