package com.ec.application.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Category;
import com.ec.application.model.Product;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ProductRepo;


@Service
public class CategoryService 
{

	@Autowired
	CategoryRepo categoryRepo;
	
	@Autowired
	ProductRepo pRepo;
	
	public Page<Category> findAll(Pageable pageable)
	{
		return categoryRepo.findAll(pageable);
    }
	
	public Category createCategory(Category payload) throws Exception 
	{
		if(!categoryRepo.existsByCategoryName(payload.getCategoryName()))
		{
			categoryRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Category already exists!");
		}
    }

	public Category updateCategory(Long id, Category payload) throws Exception 
	{
		Optional<Category> CategoryForUpdateOpt = categoryRepo.findById(id);
        Category CategoryForUpdate = CategoryForUpdateOpt.get();
        
		Category newCategory = new Category();
        newCategory = payload;
        if(!categoryRepo.existsByCategoryName(newCategory.getCategoryName()) && 
        		!newCategory.getCategoryName().equalsIgnoreCase(CategoryForUpdate.getCategoryName()))
        {		
        	CategoryForUpdate.setCategoryName(newCategory.getCategoryName());
            CategoryForUpdate.setCategoryDescription(newCategory.getCategoryDescription());
        }
        else if(newCategory.getCategoryName().equalsIgnoreCase(CategoryForUpdate.getCategoryName()))
        {
        	CategoryForUpdate.setCategoryDescription(newCategory.getCategoryDescription());
        }
        else 
        {
        	throw new Exception("Category with same Name already exists");
        }
        	
        return categoryRepo.save(CategoryForUpdate);
        
    }

	public Category findSingleCategory(Long id) 
	{
		Optional<Category> Categorys = categoryRepo.findById(id);
		return Categorys.get();
	}
	public void deleteCategory(Long id) throws Exception 
	{
		try
		{
			ArrayList<Product> products = pRepo.existsByCategoryId(id);
			if(products.size()>0)
				throw new Exception("Cannot delete category. Category already assigned to product");
			categoryRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Category");
		}
	}

	public ArrayList<Category> findCategorysByName(String name) 
	{
		ArrayList<Category> categorytList = new ArrayList<Category>();
		categorytList = categoryRepo.findBycategoryName(name);
		return categorytList;
	}

	public ArrayList<Category> findCategorysByPartialName(String name) 
	{
		return categoryRepo.findByPartialName(name);
	}

	public ArrayList<?> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return categoryRepo.findIdAndNames();
	}
}
