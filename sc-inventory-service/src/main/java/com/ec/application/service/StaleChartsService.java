package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.IndentStaleBucketChartDTO;
import com.ec.application.data.POStaleBucketChartDTO;
import com.ec.application.data.StaleAgeBucket;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@UseDefaultTenant
public class StaleChartsService {

    @Autowired
    private IndentInventoryRepo indentInventoryRepo;

    @Autowired
    private PurchaseOrderRepo purchaseOrderRepo;

    @Transactional(readOnly = true)
    public List<IndentStaleBucketChartDTO> getStaleIndentStackedChart() {

        Map<String, IndentStaleBucketChartDTO> bucketMap = new LinkedHashMap<String, IndentStaleBucketChartDTO>();

        for (StaleAgeBucket bucket : StaleAgeBucket.values()) {
            bucketMap.put(bucket.name(), new IndentStaleBucketChartDTO(bucket.name()));
        }

        List<Object[]> rows = indentInventoryRepo.fetchStaleIndentBucketData(IndentStatusConstants.getTerminalStatuses());

        for (Object[] r : rows) {
            String tenant = (String) r[0];
            String bucket = (String) r[1];
            Long count = ((Number) r[2]).longValue();

            IndentStaleBucketChartDTO dto = bucketMap.get(bucket);
            if (dto != null) {
                dto.addTenant(tenant, count);
            }
        }

        return new ArrayList<IndentStaleBucketChartDTO>(bucketMap.values());
    }

    @Transactional(readOnly = true)
    public List<POStaleBucketChartDTO> getStalePOStackedChart() {

        Map<String, POStaleBucketChartDTO> bucketMap = new LinkedHashMap<String, POStaleBucketChartDTO>();

        for (StaleAgeBucket bucket : StaleAgeBucket.values()) {
            bucketMap.put(bucket.name(), new POStaleBucketChartDTO(bucket.name()));
        }

        List<Object[]> rows = purchaseOrderRepo.fetchStalePOBucketData(POStatusConstants.getTerminalStatuses());

        for (Object[] r : rows) {
            String supplier = (String) r[0];
            String bucket = (String) r[1];
            Long count = ((Number) r[2]).longValue();

            POStaleBucketChartDTO dto = bucketMap.get(bucket);
            if (dto != null) {
                dto.addSupplier(supplier, count);
            }
        }

        return new ArrayList<>(bucketMap.values());
    }
}
