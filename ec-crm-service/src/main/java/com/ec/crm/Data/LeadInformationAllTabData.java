package com.ec.crm.Data;

import java.util.Date;

import com.ec.crm.Model.Note;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class LeadInformationAllTabData
{
	String type;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	Date dateTime;

	@JsonSerialize(using = ToUsernameSerializer.class)
	Long creator;
	Note note;
	LeadActivityOnLeadInformationDTO leadActivity;
}
