package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.data.ProductCreateData;
import com.ec.application.model.BasicEntities.Category;
import com.ec.application.model.BasicEntities.Product;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ProductRepo;


@Service
public class ProductService 
{

	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	CategoryRepo categoryRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	public Page<Product> findAll(Pageable pageable)
	{
		return productRepo.findAll(pageable);
    }
	
	public Product createProduct(ProductCreateData payload) throws Exception 
	{
		if(!productRepo.existsByProductName(payload.getProductName()))
		{
			Optional<Category> categoryOpt = categoryRepo.findById(payload.getCategoryId());
			if(categoryOpt.isPresent())
			{
				Product product = new Product();
				product.setCategory(categoryOpt.get());
				product.setMeasurementUnit(payload.getMeasurementUnit());
				product.setProductDescription(payload.getProductDescription());
				product.setProductName(payload.getProductName());
				productRepo.save(product);
				return product;
			}
			else
			{
				throw new Exception("Category with categoryid not found");
			}
		}
		else
		{
			throw new Exception("Product already exists!");
		}
    }

	public Product updateProduct(Long id, ProductCreateData payload) throws Exception 
	{
		Optional<Product> ProductForUpdateOpt = productRepo.findById(id);
		if(!ProductForUpdateOpt.isPresent())
			throw new Exception("Product not found with productid");
		Optional<Category> categoryOpt = categoryRepo.findById(payload.getCategoryId());
		if(!categoryOpt.isPresent())
			throw new Exception("Category with ID not found");
		
        Product ProductForUpdate = ProductForUpdateOpt.get();
        
        if(!productRepo.existsByProductName(payload.getProductName())
        		&& !payload.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
        {		
        		ProductForUpdate.setProductName(payload.getProductName());
            ProductForUpdate.setProductDescription(payload.getProductDescription());
            ProductForUpdate.setMeasurementUnit(payload.getMeasurementUnit());
            ProductForUpdate.setCategory(categoryOpt.get());
        }
        else if(payload.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
        {
        		ProductForUpdate.setProductDescription(payload.getProductDescription());
        		ProductForUpdate.setMeasurementUnit(payload.getMeasurementUnit());
        		ProductForUpdate.setCategory(categoryOpt.get());
        }
        else 
        {
        	throw new Exception("Product with same Name already exists");
        }
        	
        return productRepo.save(ProductForUpdate);
        
    }

	public Product findSingleProduct(Long id) throws Exception 
	{
		Product product = new Product();
		Optional<Product> productOpt = productRepo.findById(id);
		if(!productOpt.isPresent())
			throw new Exception("Product Not Found With product ID");
		else
			product = productOpt.get();
		return product;
	}
	public void deleteProduct(Long id) throws Exception 
	{
		if(!checkBeforeDeleteService.isProductUsed(id))
				productRepo.softDeleteById(id);
			else
				throw new Exception("Cannot Delete. Product already in use");
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

	public ArrayList<String> returnNameByCategory(String categoryname) 
	{
		return productRepo.returnNameByCategory(categoryname);
	}

	public List<IdNameProjections> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return productRepo.findIdAndNames();
	}
	
	public boolean checkIfProductExists(Long id)
	{
		Optional<Product> Products = productRepo.findById(id);
		if(Products.isPresent())
			return true;
		else
			return false;
	}
}
