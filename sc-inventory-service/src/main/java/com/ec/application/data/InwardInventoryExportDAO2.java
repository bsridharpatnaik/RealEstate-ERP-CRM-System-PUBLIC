package com.ec.application.data;

import java.util.Date;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
@JsonPropertyOrder(
{ "Inward ID", "Date", "MRN/GRN", "Warehouse", "Supplier", "Purchase Order Date", "Purchase Order NO","Category", "Inventory",
		"Quantity", "Measurement Unit", "Closing Stock", "Vehicle No" })
public class InwardInventoryExportDAO2
{
	@JsonProperty("Inward ID")
	Long inwardid;

	@JsonProperty("Date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date date;

	@JsonProperty("MRN/GRN")
	String MRN_GRN;

	@JsonProperty("Category")
	String category;

	@JsonProperty("Warehouse")
	String warehouse;

	@JsonProperty("Supplier")
	String supplier;

	@JsonProperty("Purchase Order Date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date purchaseOrderDate;

	@JsonProperty("Purchase Order NO")
	String purchaseOrderNO;

	@JsonProperty("Inventory")
	String inventory;

	@JsonProperty("Quantity")
	Double quantity;

	@JsonProperty("Measurement Unit")
	String measurementUnit;

	@JsonProperty("Closing Stock")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;

	@JsonProperty("Vehicle No")
	String vehicleNo;

	public InwardInventoryExportDAO2(InwardInventory ii, InwardOutwardList iol)
	{
		super();
		this.inwardid = ii.getInwardid();
		this.date = ii.getDate();
		this.vehicleNo = ii.getVehicleNo() == null ? "" : ii.getVehicleNo();
		this.quantity = iol.getQuantity();
		this.closingStock = iol.getClosingStock();
		this.measurementUnit = iol.getProduct().getMeasurementUnit();
		this.warehouse = ii.getWarehouse().getWarehouseName();
		this.supplier = ii.getSupplier().getName();
		this.MRN_GRN = ii.getOurSlipNo() == null ? "" : ii.getOurSlipNo();
		this.purchaseOrderDate = ii.getPurchaseOrderdate() == null ? null : ii.getPurchaseOrderdate();
		this.purchaseOrderNO = ii.getPurchaseOrder() == null ? "" : ii.getPurchaseOrder();
		this.inventory = iol.getProduct().getProductName();
		this.category=iol.getProduct().getCategory().getCategoryName();
	}
}
