package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Sentiment;


public class PropertyTypeListWithTypeAheadData 
{
	Page<PropertyType> propertyTypeDetails;

	public Page<PropertyType> getPropertyTypeDetails() {
		return propertyTypeDetails;
	}

	public void setPropertyTypeDetails(Page<PropertyType> propertyTypeDetails) {
		this.propertyTypeDetails = propertyTypeDetails;
	}

	

	
	
	
	
}
