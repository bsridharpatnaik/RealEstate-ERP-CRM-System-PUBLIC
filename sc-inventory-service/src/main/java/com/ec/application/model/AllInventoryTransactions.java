package com.ec.application.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

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
@Table(name = "all_inventory", indexes = {
		@Index(name = "idx_id", columnList = "id"),
		@Index(name = "idx_type", columnList = "type"),
		@Index(name = "idx_keyid", columnList = "keyid"),
		@Index(name = "idx_entryid", columnList = "entryid"),
		@Index(name = "idx_category_name", columnList = "category_name"),
		@Index(name = "idx_date", columnList = "date"),
		@Index(name = "idx_contactid", columnList = "contactid"),
		@Index(name = "idx_product_name", columnList = "product_name"),
		@Index(name = "idx_measurementunit", columnList = "measurementunit"),
		@Index(name = "idx_warehouseid", columnList = "warehouseid"),
		@Index(name = "idx_productid", columnList = "productid"),
		@Index(name = "idx_quantity", columnList = "quantity"),
		@Index(name = "idx_name", columnList = "name"),
		@Index(name = "idx_mobileno", columnList = "mobileno"),
		@Index(name = "idx_emailid", columnList = "emailid"),
		@Index(name = "idx_contacttype", columnList = "contacttype"),
		@Index(name = "idx_warehousename", columnList = "warehousename"),
		@Index(name = "idx_creationDate", columnList = "creationDate"),
		@Index(name = "idx_lastModifiedDate", columnList = "lastModifiedDate"),
		@Index(name = "idx_closingStock", columnList = "closingStock")
})
@Audited
@Data
public class AllInventoryTransactions implements Serializable {

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

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	@Column(name = "closingStock")
	Double closingStock;
}