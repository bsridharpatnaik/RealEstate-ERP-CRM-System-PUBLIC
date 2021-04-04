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
@AllArgsConstructor
@NoArgsConstructor
public class CreateDealStructureDTO
{
	Long leadId;
	Long PropertyId;
	Long propertyTypeId;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String phase;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date bookingDate;

	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String mode;
	Double amount;
	String details;
}
