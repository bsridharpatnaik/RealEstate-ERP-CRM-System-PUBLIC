package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.ReusableClasses.AssigneeComparator;

public class PopulateAssigneeList
{
	List<AssigneeDAO> assigneeDetails = new ArrayList<AssigneeDAO>();

	public List<AssigneeDAO> getAssigneeDetails()
	{
		return assigneeDetails;
	}

	public void setAssigneeDetails(List<AssigneeDAO> assigneeDetails)
	{
		this.assigneeDetails = assigneeDetails;
	}

	public PopulateAssigneeList(List<UserReturnData> list)
	{
		for (UserReturnData userReturnData : list)
		{
			AssigneeDAO assigneeDAO = new AssigneeDAO(userReturnData.getId(), userReturnData.getUsername());
			this.assigneeDetails.add(assigneeDAO);
		}
		assigneeDetails.sort(new AssigneeComparator());
	}
}