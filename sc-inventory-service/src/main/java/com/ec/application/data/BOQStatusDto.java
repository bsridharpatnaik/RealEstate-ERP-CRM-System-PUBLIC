package com.ec.application.data;

import lombok.Data;

@Data
public class BOQStatusDto {

	private String category;
	private String inventory;
	private double boqQuantity;
	private double outwardQuantity;
	private String finalLocation;
	private int status;
	
	
	
}
