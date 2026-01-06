package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsolidatedIndentLineDTO {
    private String tenantName;
    private String tenantSchemaCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date indentDate;
    private String indentNo;
    private String lineItemCode;
    private String categoryName;
    private Long productId;
    private String productName;
    private String measurementUnit;
    private Double quantity;
    private String specification;
    private String remarks;
    private String lineItemStatus;
}
