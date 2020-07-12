package com.ec.application.data;

import java.util.Date;

import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.annotation.JsonFormat;

public class InwardInventoryExportDAO2 
{
	Long inwardid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date date;
	String vehicleNo;
	String supplierSlipNo;
	String ourSlipNo;
	String product;
	Double quantity;
	Double closingStock;
	String measurementUnit;
	String warehouse;
	String supplier;
	String additionalInfo;
	Boolean invoiceReceived;
	
	public InwardInventoryExportDAO2(InwardInventory ii, InwardOutwardList iol) {
		super();
		this.inwardid=ii.getInwardid();
		this.date=ii.getDate();
		this.vehicleNo=ii.getVehicleNo();
		this.supplierSlipNo=ii.getSupplierSlipNo();
		this.ourSlipNo=ii.getOurSlipNo();
		this.product=iol.getProduct().getProductName();
		this.quantity=iol.getQuantity();
		this.closingStock=iol.getClosingStock();
		this.measurementUnit=iol.getProduct().getMeasurementUnit();
		this.warehouse=ii.getWarehouse().getWarehouseName();
		this.supplier=ii.getSupplier().getName();
		this.additionalInfo=ii.getAdditionalInfo();
		this.invoiceReceived=ii.getInvoiceReceived();
	}
	public Long getInwardid() {
		return inwardid;
	}
	public void setInwardid(Long inwardid) {
		this.inwardid = inwardid;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
	public String getSupplierSlipNo() {
		return supplierSlipNo;
	}
	public void setSupplierSlipNo(String supplierSlipNo) {
		this.supplierSlipNo = supplierSlipNo;
	}
	public String getOurSlipNo() {
		return ourSlipNo;
	}
	public void setOurSlipNo(String ourSlipNo) {
		this.ourSlipNo = ourSlipNo;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
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
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
	}
	public String getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}
	public String getSupplier() {
		return supplier;
	}
	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	public Boolean getInvoiceReceived() {
		return invoiceReceived;
	}
	public void setInvoiceReceived(Boolean invoiceReceived) {
		this.invoiceReceived = invoiceReceived;
	}
}
