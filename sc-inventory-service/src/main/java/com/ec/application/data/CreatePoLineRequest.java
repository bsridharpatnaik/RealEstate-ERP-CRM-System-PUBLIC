package com.ec.application.data;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

@Data
public class CreatePoLineRequest {

    private String lineItemCode;
    @JsonDeserialize(using= ToTitleCaseDeserializer.class)
    private String brand;
    private String grade;
    private String diameter;
    private String specification;
    private Double rate;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;
    /** One or more indent line items clubbed */
    private List<IndentLineRefRequest> indentRefs;
}
