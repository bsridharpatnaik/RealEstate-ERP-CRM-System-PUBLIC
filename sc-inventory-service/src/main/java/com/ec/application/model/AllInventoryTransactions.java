package com.ec.application.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Subselect("select * from all_inventory")
@Immutable
//@Table(name = "all_inventory")
@Audited
public class AllInventoryTransactions implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	String id;

	@Column(name = "type")
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String type;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "date")
	Date date;

	@Column(name = "contactid")
	Long contactId;

	@Column(name = "product_name")
	String productName;

	@Column(name = "measurementunit")
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String measurementUnit;

	@Column(name = "warehouseid")
	Long warehouseId;

	@Column(name = "productid")
	Long productId;

	@Column(name = "quantity")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double quantity;

	@Column(name = "closingstock")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;

	@Column(name = "name")
	String name;

	@Column(name = "mobileno")
	String mobileNo;

	@Column(name = "emailid")
	String emailId;

	@Column(name = "contacttype")
	String contactType;

	@Column(name = "warehousename")
	String warehouseName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "creationDate")
	String creationDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "lastModifiedDate")
	String lastModifiedDate;

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public Long getContactId()
	{
		return contactId;
	}

	public void setContactId(Long contactId)
	{
		this.contactId = contactId;
	}

	public String getProductName()
	{
		return productName;
	}

	public void setProductName(String productName)
	{
		this.productName = productName;
	}

	public String getMeasurementUnit()
	{
		return measurementUnit;
	}

	public void setMeasurementUnit(String measurementUnit)
	{
		this.measurementUnit = measurementUnit;
	}

	public Long getWarehouseId()
	{
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId)
	{
		this.warehouseId = warehouseId;
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

	public Double getClosingStock()
	{
		return closingStock;
	}

	public void setClosingStock(Double closingStock)
	{
		this.closingStock = closingStock;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getMobileNo()
	{
		return mobileNo;
	}

	public void setMobileNo(String mobileNo)
	{
		this.mobileNo = mobileNo;
	}

	public String getEmailId()
	{
		return emailId;
	}

	public void setEmailId(String emailId)
	{
		this.emailId = emailId;
	}

	public String getContactType()
	{
		return contactType;
	}

	public void setContactType(String contactType)
	{
		this.contactType = contactType;
	}

	public String getWarehouseName()
	{
		return warehouseName;
	}

	public void setWarehouseName(String warehouseName)
	{
		this.warehouseName = warehouseName;
	}

	public String getCreated()
	{
		return creationDate;
	}

	public void setCreated(String created)
	{
		this.creationDate = created;
	}

	public String getUpdated()
	{
		return lastModifiedDate;
	}

	public void setUpdated(String updated)
	{
		this.lastModifiedDate = updated;
	}
}
