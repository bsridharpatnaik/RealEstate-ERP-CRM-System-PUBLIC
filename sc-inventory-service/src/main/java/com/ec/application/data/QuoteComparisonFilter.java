package com.ec.application.data;

import lombok.Data;

@Data
public class QuoteComparisonFilter {
    private String qcId;
    private String project;
    private String indentId;
    private String status;
    private String createdBy;
    private String supplierName;
    private String dateFrom;
    private String dateTo;
}
