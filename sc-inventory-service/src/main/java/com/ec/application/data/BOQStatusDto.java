package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class BOQStatusDto {

	private int id;
	private String category;
	private String inventory;
	private double boqQuantity;
	private double outwardQuantity;
	private List<BOQStatusDetailsDto> boqDetails;
	private int status;
	
	
	
	
}
