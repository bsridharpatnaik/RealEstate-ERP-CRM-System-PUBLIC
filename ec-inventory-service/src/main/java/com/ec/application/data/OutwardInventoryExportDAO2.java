package com.ec.application.data;

import java.util.Date;
import java.util.Set;

import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.OutwardInventory;
import com.fasterxml.jackson.annotation.JsonFormat;

public class OutwardInventoryExportDAO2 
{
	Long outwardid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date date;
	String purpose;
	String slipNo;
	String product;
	String measurementUnit;
	Double quantity;
	Double closingStock;
	String warehouse;
	String usageArea;
	String contractor;
	String usageLocation;
	String additionalInfo;
	public OutwardInventoryExportDAO2(OutwardInventory oi, InwardOutwardList iol) 
	{
		super();
		this.outwardid=oi.getOutwardid();
		this.date=oi.getDate();
		this.purpose=oi.getPurpose();
		this.slipNo=oi.getPurpose();
		this.product=iol.getProduct().getProductName();
		this.quantity=iol.getQuantity();
		this.closingStock=iol.getClosingStock();
		this.measurementUnit=iol.getProduct().getMeasurementUnit();
		this.warehouse=oi.getWarehouse().getWarehouseName();
		this.usageArea=oi.getUsageArea().getUsageAreaName();
		this.contractor=oi.getContractor().getName();
		this.usageLocation=oi.getUsageLocation().getLocationName();
		this.additionalInfo=oi.getAdditionalInfo();
	}
	public Long getOutwardid() {
		return outwardid;
	}
	public void setOutwardid(Long outwardid) {
		this.outwardid = outwardid;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getSlipNo() {
		return slipNo;
	}
	public void setSlipNo(String slipNo) {
		this.slipNo = slipNo;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
	}
	public Double getQuantity() {
		return quantity;
	}
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	public Double getClosingStock() {
		return closingStock;
	}
	public void setClosingStock(Double closingStock) {
		this.closingStock = closingStock;
	}
	public String getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}
	public String getUsageArea() {
		return usageArea;
	}
	public void setUsageArea(String usageArea) {
		this.usageArea = usageArea;
	}
	public String getContractor() {
		return contractor;
	}
	public void setContractor(String contractor) {
		this.contractor = contractor;
	}
	public String getUsageLocation() {
		return usageLocation;
	}
	public void setUsageLocation(String usageLocation) {
		this.usageLocation = usageLocation;
	}
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
}
