package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.Category;

public class AllCategoriesWithNamesData 
{

	List<String> names;
	Page<Category> categories;
	public List<String> getNames() {
		return names;
	}
	public void setNames(List<String> names) {
		this.names = names;
	}
	public Page<Category> getCategories() {
		return categories;
	}
	public void setCategories(Page<com.ec.application.model.Category> page) {
		this.categories = page;
	}
	
}
