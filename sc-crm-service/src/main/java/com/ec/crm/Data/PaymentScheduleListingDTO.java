package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Data
public class PaymentScheduleListingDTO
{

	Long scheduleId;
	Long customerId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentDate;
	String customerName;
	String mode;
	String propertyName;
	@JsonSerialize(using = ToUsernameSerializer.class)
	Long assignee;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double amount;

	String details;

	Boolean isReceived;

	Long dealStructureId;

	Long leadActivityId;

}
