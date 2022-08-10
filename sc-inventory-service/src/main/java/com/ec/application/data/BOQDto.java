package com.ec.application.data;

import java.util.List;

import lombok.Data;

@Data
public class BOQDto {
	private int id;
	private int sno;
	private List<BOQUploadDto> upload;
	

}
