package com.ec.application.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "inward_inventory")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class InwardInventory extends ReusableFields
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="inwardid")
	Long inwardid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date date;
	
	@NonNull
	String vehicleNo;
	
	String supplierSlipNo;
	
	String ourSlipNo;
	
	@ManyToMany(fetch=FetchType.EAGER,cascade = CascadeType.ALL)
	@JoinTable(name = "inwardinventory_entry", joinColumns = {
			@JoinColumn(name = "inwardid", referencedColumnName = "inwardid") }, inverseJoinColumns = {
					@JoinColumn(name = "entryId", referencedColumnName = "entryId") })
	Set<InwardOutwardList> inwardOutwardList = new HashSet<>();;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="warehouse_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Warehouse warehouse;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="contactId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Supplier supplier;
	
	String additionalInfo;
	
	@NonNull
	@Column(nullable = false)
	Boolean invoiceReceived;
	
	public Boolean getInvoiceReceived() {
		return invoiceReceived;
	}
	public void setInvoiceReceived(Boolean invoiceReceived) {
		this.invoiceReceived = invoiceReceived;
	}
	public Long getInwardid() {
		return inwardid;
	}
	public void setInwardid(Long inwardid) {
		this.inwardid = inwardid;
	}
	public String getSupplierSlipNo() {
		return supplierSlipNo;
	}
	public void setSupplierSlipNo(String supplierSlipNo) {
		this.supplierSlipNo = supplierSlipNo;
	}
	public Set<InwardOutwardList> getInwardOutwardList() {
		return inwardOutwardList;
	}
	public void setInwardOutwardList(Set<InwardOutwardList> inwardOutwardList) {
		this.inwardOutwardList = inwardOutwardList;
	}
	public Supplier getSupplier() {
		return supplier;
	}
	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
	public String getVendorSlipNo() {
		return supplierSlipNo;
	}
	public void setVendorSlipNo(String vendorSlipNo) {
		this.supplierSlipNo = vendorSlipNo;
	}
	public String getOurSlipNo() {
		return ourSlipNo;
	}
	public void setOurSlipNo(String ourSlipNo) {
		this.ourSlipNo = ourSlipNo;
	}
	public Warehouse getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}
	
}
