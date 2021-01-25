package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import Deserializers.ToTitleCaseDeserializer;
import Deserializers.ToUpperCaseDeserializer;
import lombok.Data;

@Data
public class CreateScheduleData
{

	Long dealStructureId;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date paymentDate;

	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String mode;

	Double amount;

	String details;

	Boolean isReceived;
}
