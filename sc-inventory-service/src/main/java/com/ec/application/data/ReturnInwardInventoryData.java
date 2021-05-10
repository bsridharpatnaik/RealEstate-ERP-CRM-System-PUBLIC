package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.InwardInventory;

import lombok.Data;

@Data
public class ReturnInwardInventoryData
{
	NameAndProjectionDataForDropDown iiDropdown;
	Page<InwardInventory> inwardInventory;
	List<ProductGroupedDAO> totals;
}
