package com.ec.application.data;

import org.springframework.data.domain.Page;

import lombok.Data;

@Data
public class BOQInformation {

	 Page<BOQStatusDto> boqstatusDto; 
	 
}
