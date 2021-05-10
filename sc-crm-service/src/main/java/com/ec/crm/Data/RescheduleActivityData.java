package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class RescheduleActivityData 
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="dd-MM-yyyy HH:mm")
	Date rescheduleDateTime;
	
	String closingComment;
}
