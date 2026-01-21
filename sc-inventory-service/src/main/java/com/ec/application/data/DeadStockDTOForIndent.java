package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadStockDTOForIndent {
    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double toalDeadStock;
    List<Map<String, Double>> detailedDeadStock;
}
