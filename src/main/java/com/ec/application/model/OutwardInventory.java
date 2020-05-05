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
@Table(name = "outward_inventory")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class OutwardInventory extends ReusableFields
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="outwardid")
	Long outwardid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date Date;
	
	String purpose;
	String slipNo;
	
	@ManyToMany(fetch=FetchType.EAGER,cascade = CascadeType.ALL)
	@JoinTable(name = "outwardinventory_entry", joinColumns = {
			@JoinColumn(name = "outwardid", referencedColumnName = "outwardid") }, inverseJoinColumns = {
					@JoinColumn(name = "entryId", referencedColumnName = "entryId") })
	Set<InwardOutwardList> inwardOutwardList = new HashSet<>();;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="warehouse_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Warehouse warehouse;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="contactId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Contractor contractor;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="locationId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	UsageLocation usageLocation;
	
	String additionalInfo;
	

	public Long getOutwardid() {
		return outwardid;
	}

	public void setOutwardid(Long outwardid) {
		this.outwardid = outwardid;
	}

	public Date getDate() {
		return Date;
	}

	public void setDate(Date date) {
		Date = date;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getSlipNo() {
		return slipNo;
	}

	public void setSlipNo(String slipNo) {
		this.slipNo = slipNo;
	}

	public Set<InwardOutwardList> getInwardOutwardList() {
		return inwardOutwardList;
	}

	public void setInwardOutwardList(Set<InwardOutwardList> inwardOutwardList) {
		this.inwardOutwardList = inwardOutwardList;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Contractor getContractor() {
		return contractor;
	}

	public void setContractor(Contractor contractor) {
		this.contractor = contractor;
	}

	public UsageLocation getUsageLocation() {
		return usageLocation;
	}

	public void setUsageLocation(UsageLocation usageLocation) {
		this.usageLocation = usageLocation;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
}
