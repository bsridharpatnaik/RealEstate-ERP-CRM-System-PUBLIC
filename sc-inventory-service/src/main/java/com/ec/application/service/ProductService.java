package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.ProjectConstants;
import com.ec.application.repository.InventoryMonthPriceMappingRepository;
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
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.ProductSpecifications;

@Service
@Transactional
@UseDefaultTenant
public class ProductService {

    @Autowired
    ProductRepo productRepo;

    @Autowired
    CategoryRepo categoryRepo;

    @Autowired
    CheckBeforeDeleteService checkBeforeDeleteService;

    @Autowired
    StockService stockService;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    InventoryNotificationService inService;

    @Autowired
    InventoryMonthPriceMappingRepository inventoryMonthPriceMappingRepository;

    Logger log = LoggerFactory.getLogger(ProductService.class);

    public Page<Product> findAll(Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return productRepo.findAll(pageable);
    }

    public Product createProduct(ProductCreateData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        checkIfDashboardProductLimitReached(null, payload, "create");
        if (!productRepo.existsByProductName(payload.getProductName())) {
            Optional<Category> categoryOpt = categoryRepo.findById(payload.getCategoryId());
            if (categoryOpt.isPresent()) {
                Product product = getProduct(payload, categoryOpt);
                productRepo.save(product);
                return product;
            } else {
                throw new Exception("Category with categoryid not found");
            }
        } else {
            throw new Exception("Product already exists!");
        }
    }

    private static Product getProduct(ProductCreateData payload, Optional<Category> categoryOpt) {
        Product product = new Product();
        product.setCategory(categoryOpt.get());
        product.setMeasurementUnit(payload.getMeasurementUnit().trim());
        product.setProductDescription(
                payload.getProductDescription() == null ? "" : payload.getProductDescription().trim());
        product.setProductName(payload.getProductName().trim());
        product.setReorderQuantity(payload.getReorderQuantity());
        product.setShowOnDashboard(payload.getShowOnDashboard() != null && payload.getShowOnDashboard());
        if (payload.getIsManagedInventory() == null) {
            product.setIsManagedInventory(true);
        } else {
            product.setIsManagedInventory(payload.getIsManagedInventory());
        }
        return product;
    }

    private void checkIfDashboardProductLimitReached(Product productForUpdate, ProductCreateData payload, String action) throws Exception {

        if (action.equals("create") && payload.getShowOnDashboard()) {
            List<Product> existingDashboardProducts = productRepo.getDashboardProducts();
            if(existingDashboardProducts.size()>= ProjectConstants.noOfProductsForDashboard)
                throw new Exception("Only "+ProjectConstants.noOfProductsForDashboard+" products can be shown in dashboard. Please uncheck flag Show In Dashboard");
        } else if (action.equals("update")) {
            List<Product> existingDashboardProducts = productRepo.getDashboardProducts();
            if(payload.getShowOnDashboard() && !existingDashboardProducts.contains(productForUpdate) && existingDashboardProducts.size()>=ProjectConstants.noOfProductsForDashboard)
                throw new Exception("Only "+ProjectConstants.noOfProductsForDashboard+" products can be shown in dashboard. Please uncheck flag Show In Dashboard");
        }
    }

    private void validatePayload(ProductCreateData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getCategoryId() == null)
            throw new Exception("CategoryID cannot be empty. Please select a category");

        if (payload.getMeasurementUnit() == null)
            throw new Exception("Measurement Unit cannot be empty. Please Enter Measurement Unit");

        if (payload.getProductName() == null)
            throw new Exception("Product Name cannot be empty. Please Enter Product Name");

        if(payload.getProductName().contains(","))
            throw new Exception("Comma(,) not allowed in product name. Please enter valid product name.");

        if (payload.getReorderQuantity() == null || payload.getReorderQuantity() == 0)
            throw new Exception("Reorder Quantity cannot be zero or empty. Please Enter Reorder Quantity");

        if (payload.getProductName().length() > 50)
            throw new Exception("Product Name should not exceed 50 characters. Please provide valid product name.");
    }

    public Product updateProduct(Long id, ProductCreateData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new Exception("Product not found with productid"));

        Category category = categoryRepo.findById(payload.getCategoryId())
                .orElseThrow(() -> new Exception("Category with ID not found"));

        checkIfDashboardProductLimitReached(product, payload, "update");

        if (productRepo.existsByProductName(payload.getProductName())
                && !payload.getProductName().equalsIgnoreCase(product.getProductName())) {
            throw new Exception("Product with same Name already exists");
        }

        product.setProductName(payload.getProductName());
        product.setProductDescription(payload.getProductDescription());
        product.setMeasurementUnit(payload.getMeasurementUnit());
        product.setCategory(category);
        product.setReorderQuantity(payload.getReorderQuantity());
        product.setShowOnDashboard(Boolean.TRUE.equals(payload.getShowOnDashboard()));
        product.setIsManagedInventory(payload.getIsManagedInventory() == null || payload.getIsManagedInventory());

        return productRepo.save(product);
    }

    public Product findSingleProduct(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Product product = new Product();
        Optional<Product> productOpt = productRepo.findById(id);
        if (!productOpt.isPresent())
            throw new Exception("Product Not Found With product ID");
        else
            product = productOpt.get();
        return product;
    }

    public void deleteProduct(Long id) throws Exception {
        if (checkBeforeDeleteService.isProductNotUsedButStockExists(id)) {
            stockService.deleteStockForProduct(id);
            inService.deleteNotificationForProduct(id);
            productRepo.softDeleteById(id);
        } else if (!checkBeforeDeleteService.isProductUsed(id)) {
            inService.deleteNotificationForProduct(id);
            productRepo.softDeleteById(id);

        } else
            throw new Exception("Cannot Delete. Product already in use");
    }

    public ArrayList<Product> findProductsByName(String name) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        ArrayList<Product> productList = new ArrayList<Product>();
        productList = productRepo.findByproductName(name);
        return productList;
    }

    public List<IdNameProjections> findIdAndNames() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return productRepo.findIdAndNames();
    }

    public boolean checkIfProductExists(Long id) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<Product> Products = productRepo.findById(id);
        if (Products.isPresent())
            return true;
        else
            return false;
    }

    public Page<Product> findFilteredProductsWithTA(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<Product> spec = ProductSpecifications.getSpecification(filterDataList);
        if (spec != null)
            return productRepo.findAll(spec, pageable);
        else
            return productRepo.findAll(pageable);
    }

    public List<String> typeAheadDataList(String name) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<String> names = productRepo.getNames(name);
        names.addAll(categoryRepo.getNames(name));
        return names;
    }

    public List<IdNameProjections> getIdAndNamesForCategoryDropdown() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<IdNameProjections> categoryNamesForDropdown = categoryRepo.findIdAndNames();
        return categoryNamesForDropdown;
    }

    public List<IdNameAndUnit> productMeasurementUnit() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return productRepo.getProductMeasurementUnit();
    }
}
