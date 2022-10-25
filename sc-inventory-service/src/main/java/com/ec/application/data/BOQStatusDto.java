package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class BOQStatusDto {

	private long id;
	private String category;
	private String product;
	private double boqQuantity;
	private double outwardQuantity;
	private String buildingUnit;
	private List<BOQStatusDetailsDto> boqDetails;
	private int status;
	
	
	
}
