package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double quantity;
    private String specification;
    private String remarks;
    private String lineItemStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date creationDate;
    private DeadStockDTOForIndent deadStock;
    private CurrentStockDTOForIndent currentStock;
    private Integer leadTimeDays;
    // Non-null when this line is already referenced by an active (non-cancelled) Quote
    // Comparison — lets the indent-selection screen disable/flag it instead of letting the
    // user pick it and find out only after submitting.
    private String quoteRequestedQcId;
}
