package com.ec.application.data;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class WarehouseDTO
{
	Long warehouseId;

	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String warehouseName;
}
