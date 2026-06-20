package com.ec.application.data;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class QuoteComparisonCreateRequest {

    private String title;
    private String notes;
    private String project;
    private Date comparisonDate;

    private List<String> indentIds;

    private List<LineRequest> lines;
    private List<CriteriaRequest> criteria;

    @Data
    public static class LineRequest {
        private String indentId;
        private String indentLineId;
        private Long productId;
        private String productName;
        private String unit;
        private Double requiredQty;
        private String specifications;
        private Date needByDate;
    }

    @Data
    public static class CriteriaRequest {
        private String criteriaName;
        private String criteriaType; // NUMBER, TEXT, DATE, BOOLEAN
        private Boolean isMandatory;
        private Integer displayOrder;
    }
}
