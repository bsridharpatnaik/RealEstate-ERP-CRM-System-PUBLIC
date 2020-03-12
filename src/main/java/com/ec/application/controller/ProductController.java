package com.ec.application.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.ProductCreateData;
import com.ec.application.model.BasicEntities.Product;
import com.ec.application.service.ProductService;

@RestController
@RequestMapping("ec/product")
public class ProductController 
{
	@Autowired
	ProductService productService;
	
	@GetMapping
	public Page<Product> returnAllPayments(Pageable pageable) 
	{
		
		return productService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Product findProductbyvehicleNoProducts(@PathVariable long id) 
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
	
	@GetMapping("/name/{name}")
	public ArrayList<Product> returnCusByName(@PathVariable String name) 
	{
		return productService.findProductsByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<Product> returnCusByPartialName(@PathVariable String name) 
	{
		return productService.findProductsByPartialName(name);
	}
	@GetMapping("/idandnames")
	public ArrayList<?> returnIdAndNames() 
	{
		return productService.findIdAndNames();
	}
}
