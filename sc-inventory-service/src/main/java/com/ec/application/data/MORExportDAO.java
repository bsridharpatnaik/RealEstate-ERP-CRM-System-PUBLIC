package com.ec.application.data;

import java.util.Date;

import com.ec.application.model.MORRentModeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	Double amountCharged;

	public Long getMorid()
	{
		return morid;
	}

	public void setMorid(Long morid)
	{
		this.morid = morid;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public String getMachinery()
	{
		if (this.machinery == null)
			return "";
		else
			return machinery;
	}

	public void setMachinery(String machinery)
	{
		this.machinery = machinery;
	}

	public String getSupplier()
	{
		if (this.supplier == null)
			return "";
		else
			return supplier;
	}

	public void setSupplier(String supplier)
	{
		this.supplier = supplier;
	}

	public String getContractor()
	{
		if (this.contractor == null)
			return "";
		else
			return contractor;
	}

	public void setContractor(String contractor)
	{
		this.contractor = contractor;
	}

	public String getBuildingUnit()
	{
		if (this.buildingUnit == null)
			return "";
		else
			return buildingUnit;
	}

	public void setBuildingUnit(String buildingUnit)
	{
		this.buildingUnit = buildingUnit;
	}

	public String getVehicleNo()
	{
		if (this.vehicleNo == null)
			return "";
		else
			return vehicleNo;
	}

	public void setVehicleNo(String vehicleNo)
	{
		this.vehicleNo = vehicleNo;
	}

	public String getAdditionalNotes()
	{
		if (this.additionalNotes == null)
			return "";
		else
			return additionalNotes;
	}

	public void setAdditionalNotes(String additionalNotes)
	{
		this.additionalNotes = additionalNotes;
	}

	public MORRentModeEnum getMode()
	{
		return mode;
	}

	public void setMode(MORRentModeEnum mode)
	{
		this.mode = mode;
	}

	public Date getStartDateTime()
	{
		return startDateTime;
	}

	public void setStartDateTime(Date startDateTime)
	{
		this.startDateTime = startDateTime;
	}

	public Date getEndDateTime()
	{
		return endDateTime;
	}

	public void setEndDateTime(Date endDateTime)
	{
		this.endDateTime = endDateTime;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}

	public Double getInitialMeterReading()
	{
		if (this.rate == null)
			return (double) 0;
		else
			return initialMeterReading;
	}

	public void setInitialMeterReading(Double initialMeterReading)
	{
		this.initialMeterReading = initialMeterReading;
	}

	public Double getEndMeterReading()
	{
		if (this.rate == null)
			return (double) 0;
		else
			return endMeterReading;
	}

	public void setEndMeterReading(Double endMeterReading)
	{
		this.endMeterReading = endMeterReading;
	}

	public Double getNoOfTrips()
	{
		if (this.rate == null)
			return (double) 0;
		else
			return noOfTrips;
	}

	public void setNoOfTrips(Double noOfTrips)
	{
		this.noOfTrips = noOfTrips;
	}

	public Double getRate()
	{
		if (this.rate == null)
			return (double) 0;
		else

			return rate;
	}

	public void setRate(Double rate)
	{
		this.rate = rate;
	}

	public Double getAmountCharged()
	{
		if (this.amountCharged == null)
			return (double) 0;
		else
			return amountCharged;
	}

	public void setAmountCharged(Double amountCharged)
	{

		this.amountCharged = amountCharged;
	}

}
