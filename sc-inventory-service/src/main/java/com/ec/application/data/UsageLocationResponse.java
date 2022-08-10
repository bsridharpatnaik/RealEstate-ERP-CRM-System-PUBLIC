package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class UsageLocationResponse {
	private List<UsageLocationDto> usageLocation;
	private int usageLocationCount;
	
	
}
