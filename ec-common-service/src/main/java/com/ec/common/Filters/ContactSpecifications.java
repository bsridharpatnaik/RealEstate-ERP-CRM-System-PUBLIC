package com.ec.common.Filters;

import java.util.List;



import org.springframework.data.jpa.domain.Specification;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactAllInfo_;
import com.ec.utils.SpecificationsBuilder;

public final class ContactSpecifications 
{
	static SpecificationsBuilder<ContactAllInfo> specbldr = new SpecificationsBuilder<ContactAllInfo>();
	
	public static Specification<ContactAllInfo> getSpecification(FilterDataList filterDataList)
	{
		List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"name");
		List<String> contacttypes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"contacttype");
		List<String> mobilenumber = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"mobilenumber");
		List<String> address = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"address");
		List<String> nameormobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"nameormobile");
		
		Specification<ContactAllInfo> finalSpec = null;
		
		if(names != null && names.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.name.getName(),names));	
	
		if(contacttypes != null && contacttypes.size()>0)
		{
			if(contacttypes.contains("All"))
				feedContactTypes(contacttypes);
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.contactType.getName(),contacttypes));	
		}
		
		if(mobilenumber != null && mobilenumber.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.mobileNo.getName(),mobilenumber));	
	
		if(address != null && address.size()>0)
		{
			Specification<ContactAllInfo> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.addr_line1.getName(),address));	
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.addr_line2.getName(),address));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.city.getName(),address));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.state.getName(),address));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
		
		if(nameormobile != null && nameormobile.size()>0)
		{
			Specification<ContactAllInfo> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.name.getName(),nameormobile));	
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.mobileNo.getName(),nameormobile));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
		return finalSpec;
	}

	private static void feedContactTypes(List<String> contacttypes) 
	{
		contacttypes.clear();
		contacttypes.add("CUSTOMER");
		contacttypes.add("SUPPLIER");
		contacttypes.add("CONTRACTOR");
	}
}
