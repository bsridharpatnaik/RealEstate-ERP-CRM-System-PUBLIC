package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;

@Service
public class UtilService
{
	@Autowired
	UserDetailsService udService;

	public FilterDataList addAssigneeToFilterData(FilterDataList leadFilterDataList) throws Exception
	{
		UserReturnData currentUser = udService.getCurrentUser();
		if (!currentUser.getRoles().contains("CRM-Manager") && !currentUser.getRoles().contains("admin"))
		{
			List<String> values = new ArrayList<String>();
			values.add(currentUser.getId().toString());
			Boolean found = false;
			for (FilterAttributeData faData : leadFilterDataList.getFilterData())
			{
				if (faData.getAttrName().equalsIgnoreCase("assignee"))
				{
					faData.setAttrValue(values);
					found = true;
				}

			}
			if (!found)
			{
				FilterAttributeData faData = new FilterAttributeData();
				faData.setAttrName("assignee");
				faData.setAttrValue(values);
				leadFilterDataList.getFilterData().add(faData);
			}
		}
		return leadFilterDataList;
	}
}
