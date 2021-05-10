package com.ec.crm.Data;

import java.util.List;

import lombok.Data;

@Data
public class PlannerWithTotalReturnDAO 
{
	int totalActivities;
	List<PlannerSingleReturnDAO> activities;
}
