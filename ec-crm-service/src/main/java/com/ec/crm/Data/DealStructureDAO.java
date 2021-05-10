package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import lombok.Data;

@Data
public class DealStructureDAO
{
	Long dealId;
	String phase;
	String propertyType;
	String propertyName;

	Long propertytypeId;
	Long PropertyNameId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date bookingDate;
	String mode;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double amount;
	String details;
	Long leadId;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalReceived;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double totalPending;

	List<ScheduleReturnDAO> schedules;
}
