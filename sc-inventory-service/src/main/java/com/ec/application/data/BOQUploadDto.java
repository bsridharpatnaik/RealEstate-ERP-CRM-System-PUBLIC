package com.ec.application.data;

import lombok.Data;

@Data
public class BOQUploadDto {
	
	private int id;
	private long buildingType;
	private long buildingUnit;
	private int sno;
	private String inventory;
	private String quantity; 
	private String location;
	private String changes;
	
	
}
