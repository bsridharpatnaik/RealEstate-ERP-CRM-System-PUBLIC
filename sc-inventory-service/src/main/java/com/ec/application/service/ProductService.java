package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.IdNameAndUnit;
import com.ec.application.data.ProductCreateData;
import com.ec.application.model.Category;
import com.ec.application.model.Product;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.ProductSpecifications;

@Service
@Transactional
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

	Logger log = LoggerFactory.getLogger(ProductService.class);

	public Page<Product> findAll(Pageable pageable)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return productRepo.findAll(pageable);
	}

	public Product createProduct(ProductCreateData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		validatePayload(payload);
		if (!productRepo.existsByProductName(payload.getProductName()))
		{
			Optional<Category> categoryOpt = categoryRepo.findById(payload.getCategoryId());
			if (categoryOpt.isPresent())
			{
				Product product = new Product();
				product.setCategory(categoryOpt.get());
				product.setMeasurementUnit(payload.getMeasurementUnit().trim());
				product.setProductDescription(
						payload.getProductDescription() == null ? "" : payload.getProductDescription().trim());
				product.setProductName(payload.getProductName().trim());
				product.setReorderQuantity(payload.getReorderQuantity());
				productRepo.save(product);
				return product;
			} else
			{
				throw new Exception("Category with categoryid not found");
			}
		} else
		{
			throw new Exception("Product already exists!");
		}
	}

	private void validatePayload(ProductCreateData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (payload.getCategoryId() == null)
			throw new Exception("CategoryID cannot be empty. Please select a category");

		if (payload.getMeasurementUnit() == null)
			throw new Exception("Measurement Unit cannot be empty. Please Enter Measurement Unit");

		if (payload.getProductName() == null)
			throw new Exception("Product Name cannot be empty. Please Enter Product Name");

		if (payload.getReorderQuantity() == null || payload.getReorderQuantity() == 0)
			throw new Exception("Reorder Quantity cannot be zero or empty. Please Enter Reorder Quantity");

	}

	public Product updateProduct(Long id, ProductCreateData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		validatePayload(payload);
		Optional<Product> ProductForUpdateOpt = productRepo.findById(id);
		if (!ProductForUpdateOpt.isPresent())
			throw new Exception("Product not found with productid");
		Optional<Category> categoryOpt = categoryRepo.findById(payload.getCategoryId());
		if (!categoryOpt.isPresent())
			throw new Exception("Category with ID not found");

		Product ProductForUpdate = ProductForUpdateOpt.get();

		if (!productRepo.existsByProductName(payload.getProductName())
				&& !payload.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
		{
			ProductForUpdate.setProductName(payload.getProductName());
			ProductForUpdate.setProductDescription(payload.getProductDescription());
			ProductForUpdate.setMeasurementUnit(payload.getMeasurementUnit());
			ProductForUpdate.setCategory(categoryOpt.get());
			ProductForUpdate.setReorderQuantity(payload.getReorderQuantity());
		} else if (payload.getProductName().equalsIgnoreCase(ProductForUpdate.getProductName()))
		{
			ProductForUpdate.setProductDescription(payload.getProductDescription());
			ProductForUpdate.setMeasurementUnit(payload.getMeasurementUnit());
			ProductForUpdate.setCategory(categoryOpt.get());
			ProductForUpdate.setReorderQuantity(payload.getReorderQuantity());
		} else
		{
			throw new Exception("Product with same Name already exists");
		}

		return productRepo.save(ProductForUpdate);

	}

	public Product findSingleProduct(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Product product = new Product();
		Optional<Product> productOpt = productRepo.findById(id);
		if (!productOpt.isPresent())
			throw new Exception("Product Not Found With product ID");
		else
			product = productOpt.get();
		return product;
	}

	public void deleteProduct(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (!checkBeforeDeleteService.isProductUsed(id))
			productRepo.softDeleteById(id);
		else
			throw new Exception("Cannot Delete. Product already in use");
	}

	public ArrayList<Product> findProductsByName(String name)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		ArrayList<Product> productList = new ArrayList<Product>();
		productList = productRepo.findByproductName(name);
		return productList;
	}

	public List<IdNameProjections> findIdAndNames()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return productRepo.findIdAndNames();
	}

	public boolean checkIfProductExists(Long id)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Optional<Product> Products = productRepo.findById(id);
		if (Products.isPresent())
			return true;
		else
			return false;
	}

	public Page<Product> findFilteredProductsWithTA(FilterDataList filterDataList, Pageable pageable)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<Product> spec = ProductSpecifications.getSpecification(filterDataList);
		if (spec != null)
			return productRepo.findAll(spec, pageable);
		else
			return productRepo.findAll(pageable);
	}

	public List<String> typeAheadDataList(String name)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<String> names = productRepo.getNames(name);
		names.addAll(categoryRepo.getNames(name));
		return names;
	}

	public List<IdNameProjections> getIdAndNamesForCategoryDropdown()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<IdNameProjections> categoryNamesForDropdown = categoryRepo.findIdAndNames();
		return categoryNamesForDropdown;
	}

	public List<IdNameAndUnit> productMeasurementUnit()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return productRepo.getProductMeasurementUnit();
	}
}
