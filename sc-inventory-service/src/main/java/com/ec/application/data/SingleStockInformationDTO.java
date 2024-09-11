package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleStockInformationDTO {
    String warehouseName;
    Double quantityInHand;
    String measurementUnit;
    List<StockAgeDTO> stockAgingData;
}
