package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.data.StaleAgeBucket;
import com.ec.application.data.StaleBucketChartDTO;
import com.ec.application.repository.IndentInventoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@UseDefaultTenant
public class IndentStaleDashboardService {

    @Autowired
    private IndentInventoryRepo indentInventoryRepo;

    @Transactional(readOnly = true)
    public List<StaleBucketChartDTO> getStaleIndentStackedChart() {

        // 1️⃣ Initialize all buckets with zero values
        Map<String, StaleBucketChartDTO> bucketMap =
                new LinkedHashMap<String, StaleBucketChartDTO>();

        for (StaleAgeBucket bucket : StaleAgeBucket.values()) {
            bucketMap.put(bucket.name(), new StaleBucketChartDTO(bucket.name()));
        }

        // 2️⃣ Fetch DB data
        List<Object[]> rows =
                indentInventoryRepo.fetchStaleIndentBucketData(
                        IndentStatusConstants.getTerminalStatuses()
                );

        // 3️⃣ Overlay DB results
        for (Object[] r : rows) {
            String tenant = (String) r[0];
            String bucket = (String) r[1];
            Long count = ((Number) r[2]).longValue();

            StaleBucketChartDTO dto = bucketMap.get(bucket);
            if (dto != null) {
                dto.add(tenant, count);
            }
        }

        // 4️⃣ Return all buckets (including zero ones)
        return new ArrayList<StaleBucketChartDTO>(bucketMap.values());
    }
}
