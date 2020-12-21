package com.ec.application.data;

import org.springframework.lang.NonNull;

import lombok.Data;

@Data
public class ProductWithQuantity
{
	@NonNull
	Long productId;
	@NonNull
	Double quantity;

	String remarks;

}
