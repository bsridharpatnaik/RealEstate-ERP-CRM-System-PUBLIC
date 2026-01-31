package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.DeadStockDTOForIndent;
import com.ec.application.data.DeadStockWithDropdownData;
import com.ec.application.data.StockSummaryWithDropdownData;
import com.ec.application.model.StockSummary;
import com.ec.application.service.DeadStockService;
import com.ec.application.service.StockSummaryService;
import com.ec.application.service.StockSyncOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/stock-summary")
public class StockSummaryController {

    @Autowired
    private StockSyncOrchestrator stockSyncOrchestrator;

    @Autowired
    StockSummaryService stockSummaryService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = stockSyncOrchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public StockSummaryWithDropdownData findFilteredStockSummary(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "lastModifiedDate", direction = Sort.Direction.DESC) Pageable pageable) throws ParseException {
        return stockSummaryService.findFilteredStockSummary(filterDataList, pageable);
    }
}