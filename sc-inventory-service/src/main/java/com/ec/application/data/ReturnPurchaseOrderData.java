package com.ec.application.data;

import com.ec.application.model.IndentInventory;
import com.ec.application.model.PurchaseOrder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class ReturnPurchaseOrderData {
    NameAndProjectionDataForDropDown poDropdown;
    Page<PurchaseOrder> puchaseOrders;
}
