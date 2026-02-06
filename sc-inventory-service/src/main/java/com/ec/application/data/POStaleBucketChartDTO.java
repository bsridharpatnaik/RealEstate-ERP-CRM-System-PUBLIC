package com.ec.application.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class POStaleBucketChartDTO {

    private String bucket;                     // GT_3_DAYS, GT_7_DAYS...
    private Long total;                        // total stale POs in this bucket
    private Map<String, Long> supplierCounts;  // supplierName -> count

    public POStaleBucketChartDTO(String bucket) {
        this.bucket = bucket;
        this.total = 0L;
        this.supplierCounts = new LinkedHashMap<String, Long>();
    }

    public void addSupplier(String supplier, Long count) {
        this.supplierCounts.put(supplier, count);
        this.total += count;
    }

    public String getBucket() {
        return bucket;
    }

    public Long getTotal() {
        return total;
    }

    public Map<String, Long> getSupplierCounts() {
        return supplierCounts;
    }
}

