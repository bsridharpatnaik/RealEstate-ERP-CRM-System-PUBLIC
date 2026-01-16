package com.ec.application.data;

import java.util.Date;
import java.util.Set;

import com.ec.application.Deserializers.InwardExportSerializer;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@JsonSerialize(using = InwardExportSerializer.class)
@Data
public class InwardInventoryExportDAO {
    Long inwardid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date date;
    String vehicleNo;
    String supplierSlipNo;
    String ourSlipNo;
    Set<InwardOutwardList> inwardOutwardList;
    String warehouse;
    String supplier;
    String additionalInfo;
    Boolean invoiceReceived;


    public InwardInventoryExportDAO(InwardInventory ii) {
        super();
        this.inwardid = ii.getInwardId();
        this.date = ii.getDate();
        this.vehicleNo = ii.getVehicleNo();
        this.supplierSlipNo = ii.getSupplierSlipNo();
        this.ourSlipNo = ii.getOurSlipNo();
        this.inwardOutwardList = ii.getInwardOutwardList();
        this.supplier = ii.getSupplier().getName();
        this.additionalInfo = ii.getAdditionalInfo();
        this.invoiceReceived = ii.getInvoiceReceived();
    }
}