package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import Deserializers.ToTitleCaseDeserializer;
import Deserializers.ToUpperCaseDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleData
{

	Long dealStructureId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentDate;

	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String mode;

	Double amount;

	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String details;

	Boolean isReceived;

	Boolean isCustomerPayment;
}
