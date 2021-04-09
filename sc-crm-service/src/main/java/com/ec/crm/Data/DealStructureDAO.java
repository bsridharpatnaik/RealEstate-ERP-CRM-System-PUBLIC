package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import com.ec.crm.Enums.LoanStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Data
public class DealStructureDAO
{
	Long dealId;
	Long propertytypeId;
	Long PropertyNameId;
	String propertyType;
	String propertyName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date bookingDate;

	String details;
	Long leadId;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalReceivedCustomer;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalPendingCustomer;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalReceivedBank;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalPendingBank;

	List<ScheduleReturnDAO> schedules;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double dealAmount;
	Boolean loanRequired;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double loanAmount;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double customerAmount;
	String bankName;
	@Enumerated(EnumType.STRING)
	LoanStatusEnum loanStatus;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double supplementAmount;

	public Double getTotalAmount()
	{
		return supplementAmount+dealAmount;
	}
}
