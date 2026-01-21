package com.ec.application.service;

import com.ec.application.Filters.CategorySpecifications;
import com.ec.application.Filters.DeadStockSpecification;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.AllCategoriesWithNamesData;
import com.ec.application.data.DeadStockDTO;
import com.ec.application.data.DeadStockInformation;
import com.ec.application.data.DeadStockWithDropdownData;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
