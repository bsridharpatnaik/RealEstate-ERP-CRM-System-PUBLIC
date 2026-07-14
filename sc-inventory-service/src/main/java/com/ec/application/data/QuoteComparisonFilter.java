package com.ec.application.data;

import lombok.Data;

import java.util.List;

@Data
public class QuoteComparisonFilter {
    private String search; // matches QC Number or Indent ID
    private List<String> status;
    private String createdBy;
    private List<String> supplierName;
    private String dateFrom;
    private String dateTo;
}
