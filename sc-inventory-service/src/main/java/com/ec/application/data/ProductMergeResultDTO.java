package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class ProductMergeResultDTO {

    private boolean overallSuccess;
    private String message;
    private List<TenantMergeResult> tenantResults;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantMergeResult {
        private String tenantSchema;
        private boolean success;
        private String errorMessage;
    }
}
