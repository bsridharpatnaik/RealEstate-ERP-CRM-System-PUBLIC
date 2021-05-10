package com.ec.application.data;

import lombok.Data;

@Data
public class ProductDTO
{
	Long productId;

	String productName;

	String measurementUnit;

	Double reorderQuantity;

	CategoryDTO category;
}
