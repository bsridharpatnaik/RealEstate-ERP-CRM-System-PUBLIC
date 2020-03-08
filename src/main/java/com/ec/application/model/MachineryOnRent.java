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

import com.ec.application.SoftDelete.SoftDeletableEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@Entity
@Table(name = "machinery_on_rent")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class MachineryOnRent extends SoftDeletableEntity
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
	@JoinColumn(name="vendorId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Vendor vendor;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="locationId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Location location;
	
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
	Long initialMeterReading;
	@Column(nullable = false)
	@NonNull
	Long endMeterReading;
	@Column(nullable = false)
	@NonNull
	Long noOfTrips;
	Float amountCharged;
	
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
	public Vendor getVendor() {
		return vendor;
	}
	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
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
	public Long getInitialMeterReading() {
		return initialMeterReading;
	}
	public void setInitialMeterReading(Long initialMeterReading) {
		this.initialMeterReading = initialMeterReading;
	}
	public Long getEndMeterReading() {
		return endMeterReading;
	}
	public void setEndMeterReading(Long endMeterReading) {
		this.endMeterReading = endMeterReading;
	}
	public Long getNoOfTrips() {
		return noOfTrips;
	}
	public void setNoOfTrips(Long noOfTrips) {
		this.noOfTrips = noOfTrips;
	}
	public Float getAmountCharged() {
		return amountCharged;
	}
	public void setAmountCharged(Float amountCharged) {
		this.amountCharged = amountCharged;
	}
	
}
