package com.ec.application.data;
import lombok.Data;
import java.util.Date;

@Data
public class PoDropdownItem {

    private String purchaseOrderNumber;
    private Date poDate;
    private String supplierName;
}
