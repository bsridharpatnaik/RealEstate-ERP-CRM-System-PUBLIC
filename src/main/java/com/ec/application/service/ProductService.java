package com.ec.application.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Product;
import com.ec.application.repository.ProductRepo;


@Service
public class ProductService 
{

	@Autowired
	ProductRepo productRepo;
	
	public Page<Product> findAll(Pageable pageable)
	{
		return productRepo.findAll(pageable);
    }
	
	public Product createProduct(Product payload) throws Exception 
	{
		if(!productRepo.existsByProductName(payload.getProductName()))
		{
			productRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Product already exists!");
		}
    }

	public Product updateProduct(Long id, Product payload) throws Exception 
	{
		Optional<Product> ProductForUpdateOpt = productRepo.findById(id);
        Product ProductForUpdate = ProductForUpdateOpt.get();
        
		Product newProduct = new Product();
        newProduct = payload;
        if(!productRepo.existsByProductName(newProduct.getProductName())
        		&& !newProduct.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
        {		
        	ProductForUpdate.setProductName(newProduct.getProductName());
            ProductForUpdate.setProductDescription(newProduct.getProductDescription());
            ProductForUpdate.setMeasurementUnit(newProduct.getMeasurementUnit());
        }
        else if(newProduct.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
        {
        	ProductForUpdate.setProductDescription(newProduct.getProductDescription());
        	ProductForUpdate.setMeasurementUnit(newProduct.getMeasurementUnit());
        }
        else 
        {
        	throw new Exception("Product with same Name already exists");
        }
        	
        return productRepo.save(ProductForUpdate);
        
    }

	public Product findSingleProduct(Long id) 
	{
		Optional<Product> Products = productRepo.findById(id);
		return Products.get();
	}
	public void deleteProduct(Long id) throws Exception 
	{
		try
		{
			productRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Product");
		}
	}

	public ArrayList<Product> findProductsByName(String name) 
	{
		ArrayList<Product> productList = new ArrayList<Product>();
		productList = productRepo.findByproductName(name);
		return productList;
	}

	public ArrayList<Product> findProductsByPartialName(String name) 
	{
		return productRepo.findByPartialName(name);
	}
}
