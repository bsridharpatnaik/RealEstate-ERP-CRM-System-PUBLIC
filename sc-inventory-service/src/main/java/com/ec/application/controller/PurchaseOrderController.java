package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.FilterIndentAttributeData;
import com.ec.application.data.IndentInventoryDto;
import com.ec.application.data.IndentInventoryResponse;
import com.ec.application.data.IndentResponse;
import com.ec.application.service.PurchaseOrderService;



@RestController
@RequestMapping("/purchaseOrder")
public class PurchaseOrderController {

	@Autowired
	PurchaseOrderService purchaseOrderService;
	
	@PostMapping("/add_indent")
	public IndentResponse addIndent(@RequestBody List<IndentInventoryDto> indentInventory) throws Exception
	{
		return purchaseOrderService.addIndent(indentInventory);
	} 
	
	@GetMapping("/get_list_of_indent")
	public IndentResponse getListOfIndent() throws Exception
	{
		System.out.println("hello");
		
		return purchaseOrderService.getListOfIndent();
	}
	
	@GetMapping("/view_indent/{indentNumber}")
	public IndentInventoryResponse viewIndent(@PathVariable("indentNumber") long indentNumber) throws Exception
	{	
		return purchaseOrderService.viewIndent(indentNumber);
	} 
	
	@PutMapping("/update_indent")
	public IndentInventoryResponse updateIndent(@RequestBody List<IndentInventoryDto> indentInventory) throws Exception
	{	
		return purchaseOrderService.updateIndent(indentInventory);
	} 
	
	@DeleteMapping("/delete_indent/{indentNumber}")
	public IndentInventoryResponse deleteIndent(@PathVariable("indentNumber") long indentNumber) throws Exception
	{	
		return purchaseOrderService.deleteIndent(indentNumber);
	}

	@PostMapping("/search_by_indentNumber")
	public IndentResponse searchByIndentNumber(@RequestBody FilterIndentAttributeData filterAttributeData) throws Exception
	{	
		System.out.println(filterAttributeData.getAttrValue());
		return purchaseOrderService.searchByIndentNumber(filterAttributeData);
	}
	
	@GetMapping("/get_list_of_indentNumber")
	public List<IdNameProjections> getListOfIndentNumber() throws Exception
	{
		return purchaseOrderService.getListOfIndentNumber();
	}
	
	@GetMapping("/get_measurementunits_by_inventory/{productId}")
    public List<IdNameProjections> returnIdAndMU(@PathVariable("productId") long productId) {
        return purchaseOrderService.productMeasurementUnit(productId);
    }
}
