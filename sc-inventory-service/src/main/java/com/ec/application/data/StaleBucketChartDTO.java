package com.ec.application.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class StaleBucketChartDTO {

    private String bucket; // GT_3_DAYS, GT_7_DAYS...
    private Long total;
    private Map<String, Long> tenantCounts;

    public StaleBucketChartDTO(String bucket) {
        this.bucket = bucket;
        this.total = 0L;
        this.tenantCounts = new LinkedHashMap<String, Long>();
    }

    public void add(String tenant, Long count) {
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
