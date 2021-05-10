package com.ec.crm.Data;

import java.util.ArrayList;

import com.ec.crm.Model.PropertyName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PropertyTypeReturnDAO {
	public PropertyTypeReturnDAO() {
		// TODO Auto-generated constructor stub
	}
	Long propertyTypeId;

	private String propertyType;

	private List<PropertyName> propertyNames = new ArrayList<>();

	Long totalProperties;
	Long bookedProperties;
}
