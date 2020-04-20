package com.ec.application.data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;

public class CreateMORentData 
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	private Date date;
	
	@NonNull
	private long machineryId;
	
	@NonNull
	private long contactId;
	
	@NonNull
	private long locationId;
	
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date startDate;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date endDate;
	
	Double initialMeterReading;
	Double endMeterReading;
	Double noOfTrips;
	Double amountCharged;
	String mode;
	String vehicleNo;
	String additionalNotes;
	
	
	
	public String getAdditionalNotes() {
		return additionalNotes;
	}
	public void setAdditionalNotes(String additionalNotes) {
		this.additionalNotes = additionalNotes;
	}
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
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
	
	public long getContactId() {
		return contactId;
	}
	public void setContactId(long contactId) {
		this.contactId = contactId;
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
