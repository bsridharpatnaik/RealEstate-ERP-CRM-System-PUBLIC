package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ScheduleReturnDAO
{
	Long scheduleId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentDate;
	String mode;
	Double amount;
	String details;
	Boolean isReceived;
	Long dealStructureId;
	Long leadActivityId;
	Boolean isCustomerPayment;
}
