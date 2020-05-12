package com.ec.application.data;

import java.sql.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;

public class OutwardInventoryData 
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	Date Date;
	
	@NonNull
	Long contractorId;
	
	@NonNull
	Long usageLocationId;
	
	@NonNull
	Long warehouseId;
	
	String slipNo;
	
	@NonNull
	String purpose;
	
	@NonNull
	List<ProductWithQuantity> productWithQuantities;
	
	String additionalInfo;

	public Date getDate() {
		return Date;
	}

	public void setDate(Date date) {
		Date = date;
	}

	public Long getContractorId() {
		return contractorId;
	}

	public void setContractorId(Long contractorId) {
		this.contractorId = contractorId;
	}

	public Long getUsageLocationId() {
		return usageLocationId;
	}

	public void setUsageLocationId(Long usageLocationId) {
		this.usageLocationId = usageLocationId;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}

	public String getSlipNo() {
		return slipNo;
	}

	public void setSlipNo(String slipNo) {
		this.slipNo = slipNo;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public List<ProductWithQuantity> getProductWithQuantities() {
		return productWithQuantities;
	}

	public void setProductWithQuantities(List<ProductWithQuantity> productWithQuantities) {
		this.productWithQuantities = productWithQuantities;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

}
