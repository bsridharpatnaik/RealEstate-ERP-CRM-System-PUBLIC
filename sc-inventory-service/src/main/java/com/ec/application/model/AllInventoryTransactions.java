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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "all_inventory")
@Data
public class AllInventoryTransactions implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	String id;

	@Column(name = "type")
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String type;

	@Column(name = "sort_order")
	Integer sortOrder;

	@Column(name = "keyid")
	Long keyid;

	@Column(name = "entryid")
	Long entryid;

	@Column(name = "category_name")
	String categoryName;

	@Column(name = "date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted to match DATE type
	Date date;

	@Column(name = "contactid")
	Long contactId;

	@Column(name = "product_name")
	String productName;

	@Column(name = "measurementunit")
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String measurementUnit;

	@Column(name = "warehouse_id")
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

	@Column(name = "creationDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted format to match VARCHAR(255)
	String creationDate;

	@Column(name = "lastModifiedDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted format to match VARCHAR(255)
	String lastModifiedDate;

	@Column(name = "closingstock")
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;
}
