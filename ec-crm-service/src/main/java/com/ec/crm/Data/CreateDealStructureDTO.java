package com.ec.crm.Data;

import java.util.Date;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDealStructureDTO
{
	Long leadId;
	String PropertyName;
	PropertyTypeEnum propertyType;
	String PropertyNumber;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date bookingDate;
	String mode;
	Double amount;
	String details;
}
