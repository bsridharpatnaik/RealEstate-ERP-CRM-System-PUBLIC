package com.ec.application.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@Entity
@Table(name = "machinery_on_rent")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class MachineryOnRent extends ReusableFields
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long Id;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date Date;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="machineryId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Machinery machinery;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="contact_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Supplier supplier;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="locationId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	UsageLocation usageLocation;
	
	String vehicleNo;
	
	String mode;
	
	@JsonProperty("StartDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date startDate;
	
	@JsonProperty("EndDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date endDate;
	
	@Column(nullable = false)
	@NonNull
	Double initialMeterReading;
	@Column(nullable = false)
	@NonNull
	Double endMeterReading;
	Double noOfTrips;
	Double amountCharged;
	
	
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
	public Supplier getSupplier() {
		return supplier;
	}
	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}
	public Long getId() {
		return Id;
	}
	public void setId(Long id) {
		Id = id;
	}
	
	public Date getDate() {
		return Date;
	}
	public void setDate(Date date) {
		Date = date;
	}
	public Machinery getMachinery() {
		return machinery;
	}
	
	
	public void setMachinery(Machinery machinery) {
		this.machinery = machinery;
	}
	public UsageLocation getUsageLocation() {
		return usageLocation;
	}
	public void setUsageLocation(UsageLocation usageLocation) {
		this.usageLocation = usageLocation;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public Double getInitialMeterReading() {
		return initialMeterReading;
	}
	public void setInitialMeterReading(Double initialMeterReading) {
		this.initialMeterReading = initialMeterReading;
	}
	public Double getEndMeterReading() {
		return endMeterReading;
	}
	public void setEndMeterReading(Double endMeterReading) {
		this.endMeterReading = endMeterReading;
	}
	public Double getNoOfTrips() {
		return noOfTrips;
	}
	public void setNoOfTrips(Double noOfTrips) {
		this.noOfTrips = noOfTrips;
	}
	public Double getAmountCharged() {
		return amountCharged;
	}
	public void setAmountCharged(Double amountCharged) {
		this.amountCharged = amountCharged;
	}
}
