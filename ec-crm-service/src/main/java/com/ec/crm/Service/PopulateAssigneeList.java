package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.Data.UserReturnData;

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

	public PopulateAssigneeList(UserReturnData[] userReturnDatas)
	{
		for (UserReturnData userReturnData : userReturnDatas)
		{
			AssigneeDAO assigneeDAO = new AssigneeDAO(userReturnData.getId(), userReturnData.getUsername());
			this.assigneeDetails.add(assigneeDAO);
		}
	}
}
