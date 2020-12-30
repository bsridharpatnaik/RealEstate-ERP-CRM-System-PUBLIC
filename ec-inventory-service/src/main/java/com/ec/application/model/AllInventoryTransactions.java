package com.ec.application.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Entity
@Subselect("select * from all_inventory")
@Immutable
//@Table(name = "all_inventory")
@Audited
@Data
public class AllInventoryTransactions implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	Long id;

	@Column(name = "type")
	String type;

	@Column(name = "keyid")
	Long keyid;

	@Column(name = "entryid")
	Long entryid;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "date")
	Date date;

	@Column(name = "contactid")
	Long contactId;

	@Column(name = "product_name")
	String productName;

	@Column(name = "measurement_unit")
	String measurementUnit;

	@Column(name = "warehouseid")
	Long warehouseId;

	@Column(name = "productid")
	Long productId;

	@Column(name = "quantity")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double quantity;

	@Column(name = "closing_stock")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStockOld;

	@Column(name = "name")
	String name;

	@Column(name = "mobile_no")
	String mobileNo;

	@Column(name = "email_id")
	String emailId;

	@Column(name = "contact_type")
	String contactType;

	@Column(name = "warehouse_name")
	String warehouseName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "created_at")
	String created;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@Column(name = "updated_at")
	String updated;

	@NotAudited
	@Formula("(select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai where ai.type='outward' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id)")
	Double totalOutward;

	@NotAudited
	@Formula("(select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai where ai.type='inward' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id)")
	Double totalInward;

	@NotAudited
	@Formula("(select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai where ai.type='lost/damaged' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id)")
	Double totalLostDamaged;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	public double getClosingStock()
	{
		return this.totalInward - (this.totalOutward + this.totalLostDamaged);
	}
}
