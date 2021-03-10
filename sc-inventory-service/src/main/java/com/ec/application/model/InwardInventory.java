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
public class InwardInventory extends ReusableFields implements Cloneable
{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "inwardid")
	Long inwardid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = false)
	@NonNull
	Date date;

	@NonNull
	String vehicleNo;

	String supplierSlipNo;

	String ourSlipNo;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "inwardinventory_entry", joinColumns =
	{ @JoinColumn(name = "inwardid", referencedColumnName = "inwardid") }, inverseJoinColumns =
	{ @JoinColumn(name = "entryId", referencedColumnName = "entryId") })
	Set<InwardOutwardList> inwardOutwardList = new HashSet<>();;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "warehouse_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Warehouse warehouse;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "contactId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Supplier supplier;

	String additionalInfo;

	@NonNull
	@Column(nullable = false)
	Boolean invoiceReceived;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "inward_fileinformation", joinColumns =
	{ @JoinColumn(name = "inwardid", referencedColumnName = "inwardid") }, inverseJoinColumns =
	{ @JoinColumn(name = "id", referencedColumnName = "id") })
	Set<FileInformation> fileInformations = new HashSet<>();

	String purchaseOrder;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = true)
	Date purchaseOrderdate;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "rejectInward_entry", joinColumns =
	{ @JoinColumn(name = "inwardid", referencedColumnName = "inwardid") }, inverseJoinColumns =
	{ @JoinColumn(name = "rejectentryid", referencedColumnName = "rejectentryid") })
	Set<RejectInwardList> rejectInwardList = new HashSet<>();

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = true)
	Date challanDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = true)
	Date billDate;

	String billNo;
	String challanNo;

	public Date getChallanDate()
	{
		return challanDate;
	}

	public void setChallanDate(Date challanDate)
	{
		this.challanDate = challanDate;
	}

	public Date getBillDate()
	{
		return billDate;
	}

	public void setBillDate(Date billDate)
	{
		this.billDate = billDate;
	}

	public String getBillNo()
	{
		return billNo;
	}

	public void setBillNo(String billNo)
	{
		this.billNo = billNo;
	}

	public String getChallanNo()
	{
		return challanNo;
	}

	public void setChallanNo(String challanNo)
	{
		this.challanNo = challanNo;
	}

	public Set<RejectInwardList> getRejectInwardList()
	{
		return rejectInwardList;
	}

	public void setRejectInwardList(Set<RejectInwardList> rejectInwardList)
	{
		this.rejectInwardList = rejectInwardList;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public String getPurchaseOrder()
	{
		return purchaseOrder;
	}

	public void setPurchaseOrder(String purchaseOrder)
	{
		this.purchaseOrder = purchaseOrder;
	}

	public Date getPurchaseOrderdate()
	{
		return purchaseOrderdate;
	}

	public void setPurchaseOrderdate(Date purchaseOrderdate)
	{
		this.purchaseOrderdate = purchaseOrderdate;
	}

	public Set<FileInformation> getFileInformations()
	{
		return fileInformations;
	}

	public void setFileInformations(Set<FileInformation> fileInformations)
	{
		this.fileInformations = fileInformations;
	}

	public Boolean getInvoiceReceived()
	{
		return invoiceReceived;
	}

	public void setInvoiceReceived(Boolean invoiceReceived)
	{
		this.invoiceReceived = invoiceReceived;
	}

	public Long getInwardid()
	{
		return inwardid;
	}

	public void setInwardid(Long inwardid)
	{
		this.inwardid = inwardid;
	}

	public String getSupplierSlipNo()
	{
		return supplierSlipNo;
	}

	public void setSupplierSlipNo(String supplierSlipNo)
	{
		this.supplierSlipNo = supplierSlipNo;
	}

	public Set<InwardOutwardList> getInwardOutwardList()
	{
		return inwardOutwardList;
	}

	public void setInwardOutwardList(Set<InwardOutwardList> inwardOutwardList)
	{
		this.inwardOutwardList = inwardOutwardList;
	}

	public Supplier getSupplier()
	{
		return supplier;
	}

	public void setSupplier(Supplier supplier)
	{
		this.supplier = supplier;
	}

	public String getAdditionalInfo()
	{
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo)
	{
		this.additionalInfo = additionalInfo;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public String getVehicleNo()
	{
		return vehicleNo;
	}

	public void setVehicleNo(String vehicleNo)
	{
		this.vehicleNo = vehicleNo;
	}

	public String getVendorSlipNo()
	{
		return supplierSlipNo;
	}

	public void setVendorSlipNo(String vendorSlipNo)
	{
		this.supplierSlipNo = vendorSlipNo;
	}

	public String getOurSlipNo()
	{
		return ourSlipNo;
	}

	public void setOurSlipNo(String ourSlipNo)
	{
		this.ourSlipNo = ourSlipNo;
	}

	public Warehouse getWarehouse()
	{
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse)
	{
		this.warehouse = warehouse;
	}

}
