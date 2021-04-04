package com.ec.crm.Data;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosedLeadsListDTO
{

	Long leadId;
	String customerName;
	String primaryMobile;
	@Enumerated(EnumType.STRING)
	PropertyTypeEnum propertyType;
	@JsonSerialize(using = ToUsernameSerializer.class)
	Long asigneeId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date nextPaymentDate;
}
