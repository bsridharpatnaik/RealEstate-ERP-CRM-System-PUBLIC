package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Contact;
import com.ec.application.model.Contact_;

public final class ContactSpecifications
{
	static SpecificationsBuilder<Contact> specbldr = new SpecificationsBuilder<Contact>();

	public static Specification<Contact> getSpecification(FilterDataList filterDataList)
	{
		List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
		List<String> contacttypes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "contacttype");
		List<String> mobilenumber = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "mobilenumber");
		List<String> address = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "address");
		List<String> nameormobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "nameormobile");

		Specification<Contact> finalSpec = null;

		if (names != null && names.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Contact_.name.getName(), names));

		if (contacttypes != null && contacttypes.size() > 0)
		{
			if (contacttypes.contains("All"))
				feedContactTypes(contacttypes);
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Contact_.contactType.getName(), contacttypes));
		}

		if (mobilenumber != null && mobilenumber.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Contact_.mobileNo.getName(), mobilenumber));

		if (address != null && address.size() > 0)
		{
			Specification<Contact> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.addr_line1.getName(), address));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.addr_line2.getName(), address));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.city.getName(), address));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.state.getName(), address));
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}

		if (nameormobile != null && nameormobile.size() > 0)
		{
			Specification<Contact> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.name.getName(), nameormobile));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereDirectFieldContains(Contact_.mobileNo.getName(), nameormobile));
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}
		return finalSpec;
	}

	private static void feedContactTypes(List<String> contacttypes)
	{
		contacttypes.clear();
		contacttypes.add("SUPPLIER");
		contacttypes.add("CONTRACTOR");
	}
}
