package com.ec.application.data;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class InwardInventoryData
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@NonNull
	Date date;
	@NonNull
	Long supplierId;
	@NonNull
	Long warehouseId;
	@NonNull
	List<ProductWithQuantity> productWithQuantities;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String vehicleNo;
	String vendorSlipNo;
	String ourSlipNo;
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String additionalComments;
	String purchaseOrder;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@NonNull
	Date purchaseOrderDate;
	// @NonNull
	Boolean invoiceReceived;

	@NonNull
	List<FileInformationDAO> fileInformations;

	public String getPurchaseOrder()
	{
		return purchaseOrder;
	}

	public void setPurchaseOrder(String purchaseOrder)
	{
		this.purchaseOrder = purchaseOrder;
	}

	public Date getPurchaseOrderDate()
	{
		return purchaseOrderDate;
	}

	public void setPurchaseOrderDate(Date purchaseOrderDate)
	{
		this.purchaseOrderDate = purchaseOrderDate;
	}

	public List<FileInformationDAO> getFileInformations()
	{
		return fileInformations;
	}

	public void setFileInformations(List<FileInformationDAO> fileInformations)
	{
		this.fileInformations = fileInformations;
	}

	public Boolean getInvoiceReceived()
	{
		return invoiceReceived;
	}

	public void setInvoiceReceived(Boolean invoiceReceived)
	{
		this.invoiceReceived = invoiceReceived;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public Long getSupplierId()
	{
		return supplierId;
	}

	public void setSupplierId(Long supplierId)
	{
		this.supplierId = supplierId;
	}

	public Long getWarehouseId()
	{
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId)
	{
		this.warehouseId = warehouseId;
	}

	public List<ProductWithQuantity> getProductWithQuantities()
	{
		return productWithQuantities;
	}

	public void setProductWithQuantities(List<ProductWithQuantity> productWithQuantities)
	{
		this.productWithQuantities = productWithQuantities;
	}

	public String getVehicleNo()
	{
		return vehicleNo;
	}

	public void setVehicleNo(String vehicleNo)
	{
		this.vehicleNo = vehicleNo;
	}

	public String getVendorSlipNo()
	{
		return vendorSlipNo;
	}

	public void setVendorSlipNo(String vendorSlipNo)
	{
		this.vendorSlipNo = vendorSlipNo;
	}

	public String getOurSlipNo()
	{
		return ourSlipNo;
	}

	public void setOurSlipNo(String ourSlipNo)
	{
		this.ourSlipNo = ourSlipNo;
	}

	public String getAdditionalComments()
	{
		return additionalComments;
	}

	public void setAdditionalComments(String additionalComments)
	{
		this.additionalComments = additionalComments;
	}
}
