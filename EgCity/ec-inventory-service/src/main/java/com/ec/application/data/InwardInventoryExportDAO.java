package com.ec.application.data;

import java.util.Date;
import java.util.Set;

import com.ec.application.Deserializers.InwardExportSerializer;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = InwardExportSerializer.class)
public class InwardInventoryExportDAO 
{
	Long inwardid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
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
		this.inwardid=ii.getInwardid();
		this.date=ii.getDate();
		this.vehicleNo=ii.getVehicleNo();
		this.supplierSlipNo=ii.getSupplierSlipNo();
		this.ourSlipNo=ii.getOurSlipNo();
		this.inwardOutwardList=ii.getInwardOutwardList();
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
	public Set<InwardOutwardList> getInwardOutwardList() {
		return inwardOutwardList;
	}
	public void setInwardOutwardList(Set<InwardOutwardList> inwardOutwardList) {
		this.inwardOutwardList = inwardOutwardList;
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
