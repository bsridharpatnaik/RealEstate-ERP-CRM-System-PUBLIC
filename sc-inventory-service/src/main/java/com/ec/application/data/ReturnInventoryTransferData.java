package com.ec.application.data;

import com.ec.application.model.InventoryTransfer;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class ReturnInventoryTransferData {
    NameAndProjectionDataForDropDown itDropdown;
    Page<InventoryTransfer> inventoryTransfers;
    Integer maxAllowedInventory;
}
