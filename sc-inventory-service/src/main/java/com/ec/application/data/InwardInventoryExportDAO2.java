package com.ec.application.data;

import java.util.Date;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
public class InwardInventoryExportDAO2
{
	Long inwardid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date date;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	String warehouse;
	String supplier;
	Date purchaseOrderDate;
	String purchaseOrderNO;
	String MRN_GRN;
	String vehicleNo;
	String additionalInfo;
	Boolean invoiceReceived;
	String inventory;
	Double quantity;
	String measurementUnit;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;

	public InwardInventoryExportDAO2(InwardInventory ii, InwardOutwardList iol)
	{
		super();
		this.inwardid = ii.getInwardid();
		this.date = ii.getDate();
		this.vehicleNo = ii.getVehicleNo();
		this.quantity = iol.getQuantity();
		this.closingStock = iol.getClosingStock();
		this.measurementUnit = iol.getProduct().getMeasurementUnit();
		this.warehouse = ii.getWarehouse().getWarehouseName();
		this.supplier = ii.getSupplier().getName();
		this.additionalInfo = ii.getAdditionalInfo();
		this.invoiceReceived = ii.getInvoiceReceived();
		this.MRN_GRN = ii.getOurSlipNo();
		this.purchaseOrderDate = ii.getPurchaseOrderdate();
		this.purchaseOrderNO = ii.getPurchaseOrder();
		this.inventory = iol.getProduct().getProductName();
	}
}
