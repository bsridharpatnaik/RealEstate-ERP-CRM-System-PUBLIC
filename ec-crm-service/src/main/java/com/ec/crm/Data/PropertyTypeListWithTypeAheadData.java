package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Model.PropertyTypeEnum;
import com.ec.crm.Model.Sentiment;


public class PropertyTypeListWithTypeAheadData 
{
	Page<PropertyTypeEnum> propertyTypeDetails;

	public Page<PropertyTypeEnum> getPropertyTypeDetails() {
		return propertyTypeDetails;
	}

	public void setPropertyTypeDetails(Page<PropertyTypeEnum> propertyTypeDetails) {
		this.propertyTypeDetails = propertyTypeDetails;
	}

	

	
	
	
	
}
