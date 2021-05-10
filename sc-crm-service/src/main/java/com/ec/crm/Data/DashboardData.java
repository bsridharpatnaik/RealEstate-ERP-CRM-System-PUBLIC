package com.ec.crm.Data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class DashboardData 
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="dd-MM-yyyy")
	Date fromDate;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="dd-MM-yyyy")
	Date toDate;
	
}
