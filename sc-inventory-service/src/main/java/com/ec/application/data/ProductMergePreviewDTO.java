package com.ec.application.data;

import lombok.Data;
import java.util.List;

@Data
public class ProductMergePreviewDTO {

    private ProductInfo sourceProduct;
    private ProductInfo targetProduct;
    private List<TenantUsageSummary> tenantSummaries;
    private GlobalUsageSummary globalSummary;

    @Data
    public static class ProductInfo {
        private Long productId;
        private String productName;
        private String productCode;
        private String measurementUnit;
    }

    @Data
    public static class GlobalUsageSummary {
        private long indentEntries;
        private long purchaseOrderLineEntries;
        private long quoteComparisonLineEntries;
        private long unitConversionEntries;
    }

    @Data
    public static class TenantUsageSummary {
        private String tenantSchema;
        private long stockEntries;
        private long stockHistoryEntries;
        private long inwardOutwardEntries;
        private long rejectInwardEntries;
        private long rejectOutwardEntries;
        private long returnOutwardEntries;
        private long transferItemEntries;
        private long lostDamagedEntries;
        private long boqUploadEntries;
        private long boqInventoryEntries;
        private long boqHistoryEntries;
        private long pricingEntries;
        private long stockCommentEntries;
    }
}
