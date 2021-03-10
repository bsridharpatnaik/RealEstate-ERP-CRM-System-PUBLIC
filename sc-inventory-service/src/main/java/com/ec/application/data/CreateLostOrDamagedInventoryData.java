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
}
