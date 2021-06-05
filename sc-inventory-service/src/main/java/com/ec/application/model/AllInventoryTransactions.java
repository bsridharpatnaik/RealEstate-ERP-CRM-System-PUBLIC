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
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	String id;

	@Column(name = "type")
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String type;

	@Column(name = "keyid")
	Long keyid;

	@Column(name = "entryid")
	Long entryid;

	@Column(name="category_name")
	String categoryName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(name = "creationDate")
	String creationDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(name = "lastModifiedDate")
	String lastModifiedDate;

	@NotAudited
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	@Formula("(select "
			+ "(select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai where ai.type='inward' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id)"
			+ "- (select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai "
			+ "where ai.type='outward' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id) "
			+ "- (select (case when sum(ai.quantity) is null then 0 else sum(ai.quantity) end) from all_inventory ai where ai.type='lost-damaged' and ai.warehouseid=warehouseid and ai.productid=productid and ai.id>=id))")
	Double closingStock;

}
