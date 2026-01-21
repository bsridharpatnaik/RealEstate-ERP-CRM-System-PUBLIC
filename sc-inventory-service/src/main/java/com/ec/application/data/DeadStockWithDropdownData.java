package com.ec.application.data;

import com.ec.application.model.DeadStockSummary;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class DeadStockWithDropdownData {
    NameAndProjectionDataForDropDown deadStockDropdown;
    Page<DeadStockSummary> daeadStockSummaries;
}
