package com.ec.application.data;

import java.sql.Date;

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
	Long productId;
	
	@NonNull
	Float quantity;
	
	@NonNull
	String slipNo;
	
	@NonNull
	Long unloadingAreaId;
	
	@NonNull
	Long usageLocation;
	
	@NonNull
	String purpose;

	
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

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Float getQuantity() {
		return quantity;
	}

	public void setQuantity(Float quantity) {
		this.quantity = quantity;
	}

	public String getSlipNo() {
		return slipNo;
	}

	public void setSlipNo(String slipNo) {
		this.slipNo = slipNo;
	}

	public Long getUnloadingAreaId() {
		return unloadingAreaId;
	}

	public void setUnloadingAreaId(Long unloadingAreaId) {
		this.unloadingAreaId = unloadingAreaId;
	}

	public Long getUsageLocation() {
		return usageLocation;
	}

	public void setUsageLocation(Long usageLocation) {
		this.usageLocation = usageLocation;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
}
