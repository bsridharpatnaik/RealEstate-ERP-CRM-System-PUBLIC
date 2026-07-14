package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.StockSummaryWithDropdownData;
import com.ec.application.service.StockSummaryService;
import com.ec.application.service.StockSyncOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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

    @PostMapping("/tiles")
    public com.ec.application.data.StockSummaryTilesDTO getTiles(
            @RequestBody(required = false) com.ec.application.Filters.FilterDataList filterDataList) {
        return stockSummaryService.getTiles(filterDataList);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = stockSyncOrchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public StockSummaryWithDropdownData findFilteredStockSummary(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 10, sort = "lastModifiedDate", direction = Sort.Direction.DESC) Pageable pageable)
            throws ParseException {
        return stockSummaryService.findFilteredStockSummary(filterDataList, pageable);
    }

    /**
     * Downloads the current filtered stock summary as an Excel file.
     * The Reorder Level column is highlighted and editable — the file
     * can be modified and re-uploaded via the import endpoint.
     *
     * POST (not GET) so that the filter payload can be sent in the request body.
     */
    @PostMapping("/export/excel")
    public void exportExcel(
            @RequestBody FilterDataList filterDataList,
            HttpServletResponse response) throws Exception {
        stockSummaryService.streamExportExcel(filterDataList, response);
    }

    /**
     * Accepts an uploaded Excel file (same format as the export) and updates
     * the reorder level override for each row in the tenant's product_tenant_config table.
     *
     * Returns: { updated: N, skipped: N, errors: [...] }
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importReorderLevels(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "No file uploaded"));
        }
        Map<String, Object> result = stockSummaryService.importReorderLevels(file);
        return ResponseEntity.ok(result);
    }
}