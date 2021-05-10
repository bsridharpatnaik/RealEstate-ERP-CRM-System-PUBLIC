package com.ec.application.data;

import lombok.Data;

@Data
public class BOQStatusLocationsForType
{
	Long locationId;
	String locationName;
	Boolean crossedBOQQuantity;

	public BOQStatusLocationsForType(Long locationId, String locationName, Boolean status)
	{
		super();
		this.locationId = locationId;
		this.locationName = locationName;
		this.crossedBOQQuantity = status;
	}

}
