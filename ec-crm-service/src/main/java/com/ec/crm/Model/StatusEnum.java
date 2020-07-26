package com.ec.crm.Model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum StatusEnum implements Serializable
{
	New_Lead,
	Property_Visit,
	Negotiation,
	Deal_Closure,
	Lost_Deal;
	
	public StatusEnum setFromString(String name)
	{
		return StatusEnum.valueOf(name);
	}

}
