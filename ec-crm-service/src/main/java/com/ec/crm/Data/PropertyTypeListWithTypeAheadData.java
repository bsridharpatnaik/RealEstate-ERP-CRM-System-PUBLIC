package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Model.Sentiment;

import lombok.Data;

@Data
public class PropertyTypeListWithTypeAheadData 
{
	Page<PropertyTypeEnum> propertyTypeDetails;
}
