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
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.NAME,names));	
	
		if(contacttypes != null && contacttypes.size()>0)
		{
			if(contacttypes.contains("All"))
				feedContactTypes(contacttypes);
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.CONTACT_TYPE,contacttypes));	
		}
		
		if(mobilenumber != null && mobilenumber.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.MOBILE_NO,mobilenumber));	
	
		if(address != null && address.size()>0)
		{
			Specification<ContactAllInfo> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.ADDR_LINE1,address));	
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.ADDR_LINE2,address));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.CITY,address));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.STATE,address));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
		
		if(nameormobile != null && nameormobile.size()>0)
		{
			Specification<ContactAllInfo> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.NAME,nameormobile));	
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.MOBILE_NO,nameormobile));
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
