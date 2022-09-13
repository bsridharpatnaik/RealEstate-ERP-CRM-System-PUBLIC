package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class BOQStatusDataDto {

	private long buildingTypeId;
	private List<Long> buildingUnitIds;
		
}
