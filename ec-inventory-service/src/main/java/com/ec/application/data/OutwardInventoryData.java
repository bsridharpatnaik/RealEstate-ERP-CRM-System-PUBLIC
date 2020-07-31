package com.ec.application.data;

import java.sql.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

public class OutwardInventoryData 
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	Date date;
	
	@NonNull
	Long contractorId;
	
	@NonNull
	Long usageLocationId;
	
	@NonNull
	Long warehouseId;
	
	String slipNo;
	
	@NonNull
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String purpose;
	
	@NonNull
	Long  usageAreaId;
	
	@NonNull
	List<ProductWithQuantity> productWithQuantities;
	
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String additionalInfo;

	@NonNull
	List<FileInformationDAO> fileInformations;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
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

	public Long getUsageAreaId() {
		return usageAreaId;
	}

	public void setUsageAreaId(Long usageAreaId) {
		this.usageAreaId = usageAreaId;
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

	public List<FileInformationDAO> getFileInformations() {
		return fileInformations;
	}

	public void setFileInformations(List<FileInformationDAO> fileInformations) {
		this.fileInformations = fileInformations;
	}
	
}
