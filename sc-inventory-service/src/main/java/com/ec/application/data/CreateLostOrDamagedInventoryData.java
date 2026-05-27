package com.ec.application.data;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class CreateLostOrDamagedInventoryData
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	private Date date;

	@NonNull
	Long productId;

	@NonNull
	Double quantity;

	@NonNull
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String theftLocation;

	@NonNull
	Long warehouseId;

	@NonNull
	List<FileInformationDAO> fileInformations;

	String additionalComment;

	String entryType = "LOST_DAMAGED";

	// Lost/Damaged: user selects which batch the loss came from
	Long batchId;

	// Excess Found: user enters batch details for the new batch
	String brand;

	String lotNumber;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date expiryDate;

	public String getEntryType()
	{
		return entryType;
	}

	public void setEntryType(String entryType)
	{
		this.entryType = entryType;
	}

	public String getAdditionalComment()
	{
		return additionalComment;
	}

	public void setAdditionalComment(String additionalComment)
	{
		this.additionalComment = additionalComment;
	}

	public List<FileInformationDAO> getFileInformations()
	{
		return fileInformations;
	}

	public void setFileInformations(List<FileInformationDAO> fileInformations)
	{
		this.fileInformations = fileInformations;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public Long getProductId()
	{
		return productId;
	}

	public void setProductId(Long productId)
	{
		this.productId = productId;
	}

	public Double getQuantity()
	{
		return quantity;
	}

	public void setQuantity(Double quantity)
	{
		this.quantity = quantity;
	}

	public String getTheftLocation()
	{
		return theftLocation;
	}

	public void setTheftLocation(String theftLocation)
	{
		this.theftLocation = theftLocation;
	}

	public Long getWarehouseId()
	{
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId)
	{
		this.warehouseId = warehouseId;
	}

	public Long getBatchId()
	{
		return batchId;
	}

	public void setBatchId(Long batchId)
	{
		this.batchId = batchId;
	}

	public String getBrand()
	{
		return brand;
	}

	public void setBrand(String brand)
	{
		this.brand = brand;
	}

	public String getLotNumber()
	{
		return lotNumber;
	}

	public void setLotNumber(String lotNumber)
	{
		this.lotNumber = lotNumber;
	}

	public Date getExpiryDate()
	{
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate)
	{
		this.expiryDate = expiryDate;
	}
}
