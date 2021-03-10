package com.ec.application.data;

import java.util.Date;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
public class RejectInwardListDTO
{
	Long rejectentryid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date rejectDate;

	ProductDTO product;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double oldQuantity;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double rejectQuantity;

	Double closingStock;

	String remarks;
}
