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
public class CurrentStockForIndentDTO {
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double totalCurrentStock;
    private List<Map<String, Double>> warehouseWiseStock; // { warehouseName: quantity }
}