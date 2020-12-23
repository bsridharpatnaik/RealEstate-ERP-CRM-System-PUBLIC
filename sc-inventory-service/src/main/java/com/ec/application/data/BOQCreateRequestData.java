package com.ec.application.data;

import com.ec.application.model.BOQLocationTypeEnum;

import lombok.Data;

@Data
public class BOQCreateRequestData
{
	BOQLocationTypeEnum boqType;
	Long productId;
	Double quantity;
	Long id;
}
