package com.ec.application.data;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SupplierQuoteRequest {

    private Long supplierId;
    private String supplierName;
    private String quotationRefNo;
    private Date quotationDate;
    private Date validityDate;
    private String paymentTerms;
    private String freightTerms;
    private Integer deliveryLeadDays;
    private String headerNotes;
    private String revisionLabel;

    // Files already uploaded via /master-file/upload
    private List<FileInformationDAO> fileInformations;

    private List<QuoteLineRequest> lines;

    // Values for criteria with scope = HEADER (one per vendor, not per line)
    private List<CriteriaValueRequest> headerCriteriaValues;

    @Data
    public static class QuoteLineRequest {
        private Long qcLineId;
        private Double quotedQty;
        private Double quotedRate;
        private Double discountPercent;
        private Double gstPercent;
        private Double freightAmount;
        private Date expectedDeliveryDate;
        private String lineRemarks;
        private List<CriteriaValueRequest> criteriaValues;
    }

    @Data
    public static class CriteriaValueRequest {
        private Long criteriaId;
        private String criteriaName;
        private String value;
    }
}
