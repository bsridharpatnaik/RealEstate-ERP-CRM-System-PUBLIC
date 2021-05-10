package com.ec.crm.Data;

import java.util.Date;

import com.ec.crm.Model.Note;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;

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

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public Date getDateTime()
	{
		return dateTime;
	}

	public void setDateTime(Date dateTime)
	{
		this.dateTime = dateTime;
	}

	public Long getCreator()
	{
		return creator;
	}

	public void setCreator(Long creator)
	{
		this.creator = creator;
	}

	public Note getNote()
	{
		return note;
	}

	public void setNote(Note note)
	{
		this.note = note;
	}

	public LeadActivityOnLeadInformationDTO getLeadActivity()
	{
		return leadActivity;
	}

	public void setLeadActivity(LeadActivityOnLeadInformationDTO leadActivity)
	{
		this.leadActivity = leadActivity;
	}

}
