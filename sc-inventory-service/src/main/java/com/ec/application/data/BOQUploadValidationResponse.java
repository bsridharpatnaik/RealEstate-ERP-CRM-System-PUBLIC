package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class BOQUploadValidationResponse {
	
	private String message;
	private int sno;
	private List<String> columns;
	
	
}


