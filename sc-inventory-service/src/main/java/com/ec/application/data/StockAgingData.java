package com.ec.application.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class StockAgingData {
    Map<String, List<StockAgeDTO>> stockAge;

    public StockAgingData() {
        this.stockAge = new HashMap<>();
    }
}
