package com.ec.application.data;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.ec.application.model.MORRentModeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

public class CreateMORentData
{

	@JsonFormat(pattern = "dd-MM-yyyy")
	@NonNull
	Date date;

	@NonNull
	private Long machineryId;

	private Long supplierId;

	private Long contractorId;

	private Long locationId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	private Date startDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	private Date endDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	private Date startDateTime;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	private Date endDateTime;

	Double initialMeterReading;
	Double endMeterReading;
	Double noOfTrips;
	Double amountCharged;
	@NonNull
	MORRentModeEnum mode;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String vehicleNo;
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String additionalNotes;
	@NonNull
	List<FileInformationDAO> fileInformations;
	Double rate;
	String mrnGrn;

	public String getMrnGrn() {
		return mrnGrn;
	}

	public void setMrnGrn(String mrnGrn) {
		this.mrnGrn = mrnGrn;
	}

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Long getMachineryId() {
		return machineryId;
	}
	public void setMachineryId(Long machineryId) {
		this.machineryId = machineryId;
	}
	public Long getSupplierId() {
		return supplierId;
	}
	public void setSupplierId(Long supplierId) {
		this.supplierId = supplierId;
	}
	public Long getContractorId() {
		return contractorId;
	}
	public void setContractorId(Long contractorId) {
		this.contractorId = contractorId;
	}
	public Long getLocationId() {
		return locationId;
	}
	public void setLocationId(Long locationId) {
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
	public Date getStartDateTime() {
		return startDateTime;
	}
	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}
	public Date getEndDateTime() {
		return endDateTime;
	}
	public void setEndDateTime(Date endDateTime) {
		this.endDateTime = endDateTime;
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
	public MORRentModeEnum getMode() {
		return mode;
	}
	public void setMode(MORRentModeEnum mode) {
		this.mode = mode;
	}
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
	public String getAdditionalNotes() {
		return additionalNotes;
	}
	public void setAdditionalNotes(String additionalNotes) {
		this.additionalNotes = additionalNotes;
	}
	public List<FileInformationDAO> getFileInformations() {
		return fileInformations;
	}
	public void setFileInformations(List<FileInformationDAO> fileInformations) {
		this.fileInformations = fileInformations;
	}
	public Double getRate() {
		return rate;
	}
	public void setRate(Double rate) {
		this.rate = rate;
	}

}
