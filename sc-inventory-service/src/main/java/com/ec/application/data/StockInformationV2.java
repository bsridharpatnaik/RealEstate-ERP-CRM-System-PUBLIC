package com.ec.application.data;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class StockInformationV2 {
    Page<StockInformationDTO> stockInformation;
}
