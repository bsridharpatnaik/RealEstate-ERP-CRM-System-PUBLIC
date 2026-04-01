package com.ec.application.data;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
public class UpdatePoLineRequest {
    private Long lineId;
    private Double rate;
    private Double discountPercent;
    private Double tolerancePercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;
    private String brand;
    private String grade;
    private String diameter;
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String specification;
    /** UUID of the DBFile used as a sample image for this line item */
    private String sampleImageFileId;
}
