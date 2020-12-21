package com.ec.application.data;

import java.util.List;

import org.springframework.lang.NonNull;

import lombok.Data;

@Data
public class ReturnRejectInwardOutwardData
{
	@NonNull
	List<ProductWithQuantity> productWithQuantities;

	String remarks;

	public ReturnRejectInwardOutwardData()
	{
		super();
		// TODO Auto-generated constructor stub
	}

}
