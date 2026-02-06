package com.ec.application.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class IndentStaleBucketChartDTO {

    private String bucket;                 // GT_3_DAYS, GT_7_DAYS...
    private Long total;                    // total stale indents in this bucket
    private Map<String, Long> tenantCounts; // tenantName -> count

    public IndentStaleBucketChartDTO(String bucket) {
        this.bucket = bucket;
        this.total = 0L;
        this.tenantCounts = new LinkedHashMap<String, Long>();
    }

    public void addTenant(String tenant, Long count) {
        this.tenantCounts.put(tenant, count);
        this.total += count;
    }

    public String getBucket() {
        return bucket;
    }

    public Long getTotal() {
        return total;
    }

    public Map<String, Long> getTenantCounts() {
        return tenantCounts;
    }
}
