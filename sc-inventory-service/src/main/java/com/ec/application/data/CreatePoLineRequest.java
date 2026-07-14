package com.ec.application.data;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

@Data
public class CreatePoLineRequest {
    private Long productId;
    private Double quantity;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String brand;
    private String grade;
    private String diameter;
    private String size;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String specification;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;
    private Double tolerancePercent = 0.0;
    private String billingUnit;
    private Double billingQuantity;
    private Double billingConversionFactor;
    /** One or more indent line items clubbed */
    private List<IndentLineRefRequest> indentRefs;
    /** UUID of the DBFile used as a sample image for this line item */
    private String sampleImageFileId;
}
