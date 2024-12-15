package com.ec.application.data;

import java.util.Date;

import com.ec.application.model.MORRentModeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;

public class MORExportDAO
{
	Long morid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date date;

	String machinery;

	String supplier;

	String contractor;

	String buildingUnit;

	String vehicleNo;

	String additionalNotes;

	String mrnGrn;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	MORRentModeEnum mode;

	@JsonProperty("startDateTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	Date startDateTime;

	@JsonProperty("endDateTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
	Date endDateTime;

	@JsonProperty("startDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date startDate;

	@JsonProperty("endDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date endDate;

	Double initialMeterReading;
	Double endMeterReading;
	Double noOfTrips;
	Double rate;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double amountCharged;

	public String getMrnGrn() 
	{
		return mrnGrn != null ? mrnGrn : "";
	}

	public void setMrnGrn(String mrnGrn) 
	{
		this.mrnGrn = mrnGrn;
	}

	public Long getMorid()
	{
		return morid != null ? morid : 0L;
	}

	public void setMorid(Long morid)
	{
		this.morid = morid;
	}

	public Date getDate()
	{
		return date != null ? date : new Date();
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public String getMachinery()
	{
		return machinery != null ? machinery : "";
	}

	public void setMachinery(String machinery)
	{
		this.machinery = machinery;
	}

	public String getSupplier()
	{
		return supplier != null ? supplier : "";
	}

	public void setSupplier(String supplier)
	{
		this.supplier = supplier;
	}

	public String getContractor()
	{
		return contractor != null ? contractor : "";
	}

	public void setContractor(String contractor)
	{
		this.contractor = contractor;
	}

	public String getBuildingUnit()
	{
		return buildingUnit != null ? buildingUnit : "";
	}

	public void setBuildingUnit(String buildingUnit)
	{
		this.buildingUnit = buildingUnit;
	}

	public String getVehicleNo()
	{
		return vehicleNo != null ? vehicleNo : "";
	}

	public void setVehicleNo(String vehicleNo)
	{
		this.vehicleNo = vehicleNo;
	}

	public String getAdditionalNotes()
	{
		return additionalNotes != null ? additionalNotes : "";
	}

	public void setAdditionalNotes(String additionalNotes)
	{
		this.additionalNotes = additionalNotes;
	}

	public MORRentModeEnum getMode()
	{
		return mode;  // Enum should be handled at service layer
	}

	public void setMode(MORRentModeEnum mode)
	{
		this.mode = mode;
	}

	public Date getStartDateTime()
	{
		return startDateTime != null ? startDateTime : null;  // Date can be null based on mode
	}

	public void setStartDateTime(Date startDateTime)
	{
		this.startDateTime = startDateTime;
	}

	public Date getEndDateTime()
	{
		return endDateTime != null ? endDateTime : null;  // Date can be null based on mode
	}

	public void setEndDateTime(Date endDateTime)
	{
		this.endDateTime = endDateTime;
	}

	public Date getStartDate()
	{
		return startDate != null ? startDate : null;  // Date can be null based on mode
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate != null ? endDate : null;  // Date can be null based on mode
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}

	public Double getInitialMeterReading()
	{
		return initialMeterReading != null ? initialMeterReading : 0.0;
	}

	public void setInitialMeterReading(Double initialMeterReading)
	{
		this.initialMeterReading = initialMeterReading;
	}

	public Double getEndMeterReading()
	{
		return endMeterReading != null ? endMeterReading : 0.0;
	}

	public void setEndMeterReading(Double endMeterReading)
	{
		this.endMeterReading = endMeterReading;
	}

	public Double getNoOfTrips()
	{
		return noOfTrips != null ? noOfTrips : 0.0;
	}

	public void setNoOfTrips(Double noOfTrips)
	{
		this.noOfTrips = noOfTrips;
	}

	public Double getRate()
	{
		return rate != null ? rate : 0.0;
	}

	public void setRate(Double rate)
	{
		this.rate = rate;
	}

	public Double getAmountCharged()
	{
		return amountCharged != null ? amountCharged : 0.0;
	}

	public void setAmountCharged(Double amountCharged)
	{
		this.amountCharged = amountCharged;
	}

}
