package com.ec.application.data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class CreateMORentData 
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date date;
	private long machineryId;
	private long vendorId;
	private long locationId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date startDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date endDate;
	Long initialMeterReading;
	Long endMeterReading;
	Long noOfTrips;
	Float amountCharged;
	String mode;
	
	
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public long getMachineryId() {
		return machineryId;
	}
	public void setMachineryId(long machineryId) {
		this.machineryId = machineryId;
	}
	public long getVendorId() {
		return vendorId;
	}
	public void setVendorId(long vendorId) {
		this.vendorId = vendorId;
	}
	public long getLocationId() {
		return locationId;
	}
	public void setLocationId(long locationId) {
		this.locationId = locationId;
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
