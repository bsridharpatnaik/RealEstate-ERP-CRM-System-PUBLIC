package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.AllCategoriesWithNamesData;
import com.ec.application.model.Category;
import com.ec.application.service.CategoryService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/category")
public class CategoryController
{
	@Autowired
	CategoryService categoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public AllCategoriesWithNamesData returnFilteredCategories(@RequestBody FilterDataList filterDataList,
			@PageableDefault(page = 0, size = 10, sort = "createdBy", direction = Direction.DESC) Pageable pageable)
	{
		return categoryService.findFilteredCategoriesWithTA(filterDataList, pageable);
	}

	@GetMapping("/{id}")
	public Category findCategorybyvehicleNoCategorys(@PathVariable long id)
	{
		return categoryService.findSingleCategory(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteCategory(@PathVariable Long id) throws Exception
	{

		categoryService.deleteCategory(id);
		return ResponseEntity.ok("Entity deleted");
	}

	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	public Category createCategory(@RequestBody Category payload) throws Exception
	{

		// String json = ReusableMethods.convertObjectToJson(payload);
		// Category c = ReusableMethods.convertJSONtoObject(json,Category.class);
		return categoryService.createCategory(payload);
	}

	@PutMapping("/{id}")
	public Category updateCategory(@PathVariable Long id, @RequestBody Category Category) throws Exception
	{
		return categoryService.updateCategory(id, Category);
	}

	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIDandNames()
	{
		return categoryService.findIdAndNames();
	}

	@ExceptionHandler(
	{ JpaSystemException.class })
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiOnlyMessageAndCodeError sqlError(Exception ex)
	{
		ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
				"Something went wrong while handling data. Contact Administrator.");
		return apiError;
	}
}
