package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
public class InwardOutwardListDTO
{
	Long entryid;
	ProductDTO product;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double quantity;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;
}
