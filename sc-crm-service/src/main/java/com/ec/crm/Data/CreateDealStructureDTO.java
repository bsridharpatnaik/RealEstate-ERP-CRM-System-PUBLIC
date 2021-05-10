package com.ec.crm.Data;

import java.util.Date;

import com.ec.crm.Enums.LoanStatusEnum;
import com.ec.crm.Model.ClosedLeads;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import Deserializers.ToTitleCaseDeserializer;
import Deserializers.ToUpperCaseDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDealStructureDTO
{
	Long leadId;
	Long PropertyId;
	Long propertyTypeId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date bookingDate;
	Double dealAmount;
	Boolean loanRequired;
	Double loanAmount;
	Double customerAmount;
	String bankName;
	@Enumerated(EnumType.STRING)
	LoanStatusEnum loanStatus;
	Double supplementAmount;
	String details;
}
