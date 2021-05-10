package com.ec.application.data;

import java.util.List;

import org.springframework.lang.NonNull;

import lombok.Data;

@Data
public class ReturnOutwardData 
{
	@NonNull
	List<ProductWithQuantity> productWithQuantities;

	public ReturnOutwardData() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
}
