package com.ec.common.Filters;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactAllInfo_;
import com.ec.utils.SpecificationsBuilder;

import lombok.NonNull;

public final class ContactSpecifications 
{
static SpecificationsBuilder<ContactAllInfo> specbldr = new SpecificationsBuilder<ContactAllInfo>();
	
	public static Specification<ContactAllInfo> getSpecification(FilterDataList filterDataList)
	{
		List<String> names = specbldr.fetchValueFromFilterList(filterDataList,"name");
		List<String> contacttypes = specbldr.fetchValueFromFilterList(filterDataList,"contacttype");
		List<String> mobilenumber = specbldr.fetchValueFromFilterList(filterDataList,"mobilenumber");
		List<String> address = specbldr.fetchValueFromFilterList(filterDataList,"address");
		List<String> nameormobile = specbldr.fetchValueFromFilterList(filterDataList,"nameormobile");
		
		Specification<ContactAllInfo> finalSpec = null;
		
		if(names != null && names.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.NAME,names));	
	
		if(contacttypes != null && contacttypes.size()>0)
		{
			if(contacttypes.contains("All"))
				feedContactTypes(contacttypes);
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(ContactAllInfo_.CONTACT_TYPE,contacttypes));	
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
	
	
	
	/*
	 * 
	 * NAME,
	CONTACTTYPE,
	MOBILENUMBER,
	ADDRESS,
	NAMEORMOBILE;
	 * public static Specification<ContactAllInfo> whereNameContains(@NonNull String name) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.name), "%"+name+"%");
    }

    public static Specification<ContactAllInfo> whereMobileNoContains(@NonNull String mobileNo) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.mobileNo), "%"+mobileNo+"%");
    }
    
    public static Specification<ContactAllInfo> whereAddress1Contains(@NonNull String address) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.addr_line1), "%"+address+"%");
    }
    
    public static Specification<ContactAllInfo> whereAddress2Contains(@NonNull String address) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.addr_line2), "%"+address+"%");
    }
    
    public static Specification<ContactAllInfo> whereCityContains(@NonNull String address) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.city), "%"+address+"%");
    }
    
    public static Specification<ContactAllInfo> whereStateContains(@NonNull String address) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.state), "%"+address+"%");
    }
    
    public static Specification<ContactAllInfo> whereZipContains(@NonNull String address) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(ContactAllInfo_.zip), "%"+address+"%");
    }
    
    public static Specification<ContactAllInfo> whereContactTypeEquals(@NonNull String contactType) {
        return (Root<ContactAllInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.equal(root.get(ContactAllInfo_.contactType), contactType);
    }*/
}
