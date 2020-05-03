package com.ec.application.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

@Entity
@Subselect("select * from all_inventory")
@Immutable
@Audited
@NamedQuery(name = "AllInventoryTransactions.findAll", query="select u from AllInventoryTransactions u order by u.created_at desc")
public class AllInventoryTransactions 
{
	
	@Id
	@Column(name="id")
	Long id;
	
	@Column(name="type")
	String type;
	
	@Column(name="date")
	Date date;
	
	@Column(name="contactid")
	Long contactId;
	
	@Column(name="warehouseid")
	Long warehouseId;
	
	@Column(name="productid")
	Long productId;
	
	@Column(name="quantity")
	Double quantity;
	
	@Column(name="closing_stock")
	Double ClosingStock;
	
	@Column(name="name")
	String name;
	
	@Column(name="mobile_no")
	Long mobileNo;
	
	@Column(name="email_id")
	String email_id;
	
	@Column(name="contact_type")
	String contact_type;
	
	@Column(name="warehouse_name")
	String warehouseName;
	
	@Column(name="created_at")
	String created_at;
	
	@Column(name="updated_at")
	String updated_at;

	

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getContactId() {
		return contactId;
	}

	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Double getQuantity() {
		return quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public Double getClosingStock() {
		return ClosingStock;
	}

	public void setClosingStock(Double closingStock) {
		ClosingStock = closingStock;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(Long mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getEmail_id() {
		return email_id;
	}

	public void setEmail_id(String email_id) {
		this.email_id = email_id;
	}

	public String getContact_type() {
		return contact_type;
	}

	public void setContact_type(String contact_type) {
		this.contact_type = contact_type;
	}

	public String getWarehouseName() {
		return warehouseName;
	}

	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}
}
