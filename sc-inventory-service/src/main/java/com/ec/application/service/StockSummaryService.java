package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.StockSummarySpecification;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
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
}
