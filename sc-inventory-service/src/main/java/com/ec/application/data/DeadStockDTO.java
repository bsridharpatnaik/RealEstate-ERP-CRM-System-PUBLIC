package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeadStockDTO {
    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double toalDealStock;
    List<Map<String, Double>> detailedDeadStock;

}
