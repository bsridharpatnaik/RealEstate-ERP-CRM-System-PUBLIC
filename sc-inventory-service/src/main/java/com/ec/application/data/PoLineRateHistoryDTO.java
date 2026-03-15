package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PoLineRateHistoryDTO {
    private Long productId;
    private String productName;
    private String productCode;
    private List<PreviousPurchaseRateDTO> previousRates;
}