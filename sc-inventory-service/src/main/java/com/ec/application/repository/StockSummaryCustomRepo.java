package com.ec.application.repository;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.ProductStockSumDTO;
import com.ec.application.data.StockSummaryAggregatedDTO;
import com.ec.application.data.StockSummaryTilesDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockSummaryCustomRepo {
    // allowedSchemas: restrict rows to the current user's tenants. Empty list = no restriction (admin/all).
    Page<StockSummaryAggregatedDTO> fetchAggregatedStock(FilterDataList filters, Pageable pageable, List<String> allowedSchemas);
    StockSummaryTilesDTO getTileCounts(FilterDataList filters, List<String> allowedSchemas);
}
