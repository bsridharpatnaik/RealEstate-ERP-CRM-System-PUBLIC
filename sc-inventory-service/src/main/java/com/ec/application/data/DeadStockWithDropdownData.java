package com.ec.application.data;

import com.ec.application.model.StockSummary;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.Date;

@Data
public class DeadStockWithDropdownData {
    NameAndProjectionDataForDropDown deadStockDropdown;
    Page<StockSummary> daeadStockSummaries;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    Date lastSyncDate;
}
