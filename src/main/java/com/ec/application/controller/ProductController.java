package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.AllProductsWithNamesData;
import com.ec.application.data.ProductCreateData;
import com.ec.application.model.Product;
import com.ec.application.service.ProductService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/product")
public class ProductController 
{
	@Autowired
	ProductService productService;
	
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public AllProductsWithNamesData returnFilteredProducts(@RequestBody FilterDataList filterDataList,@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return productService.findFilteredProductsWithTA(filterDataList,pageable);
	}
	
	@GetMapping("/{id}")
	public Product findProductbyvehicleNoProducts(@PathVariable long id) throws Exception 
	{
		return productService.findSingleProduct(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteProduct(@PathVariable Long id) throws Exception
	{
		productService.deleteProduct(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Product createProduct(@RequestBody ProductCreateData payload) throws Exception{
		
		return productService.createProduct(payload);
	}

	@PutMapping("/{id}")
	public Product updateProduct(@PathVariable Long id, @RequestBody ProductCreateData Product) throws Exception 
	{
		return productService.updateProduct(id, Product);
	} 
	
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return productService.findIdAndNames();
	}
}
