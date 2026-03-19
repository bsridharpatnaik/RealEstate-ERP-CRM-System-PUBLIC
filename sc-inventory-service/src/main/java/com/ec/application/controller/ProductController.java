package com.ec.application.controller;

import java.text.ParseException;
import java.util.List;

import com.ec.application.aspects.AllowOnly;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.constants.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.IdNameAndUnit;
import com.ec.application.data.ProductCreateData;
import com.ec.application.data.ProductTenantConfigDTO;
import com.ec.application.model.Product;
import com.ec.application.service.ProductService;
import com.ec.application.service.ProductTenantConfigService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    ProductService productService;

    @Autowired
    ProductTenantConfigService productTenantConfigService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<Product> returnFilteredProducts(@RequestBody FilterDataList filterDataList,
                                                @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
            throws ParseException {
        return productService.findFilteredProductsWithTA(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public Product findProductbyvehicleNoProducts(@PathVariable long id) throws Exception {
        return productService.findSingleProduct(id);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) throws Exception {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public Product createProduct(@RequestBody ProductCreateData payload) throws Exception {
        return productService.createProduct(payload);
    }

    @PutMapping("/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductCreateData Product) throws Exception {
        return productService.updateProduct(id, Product);
    }

    @GetMapping("/idandnames")
    public List<IdNameProjections> returnIdAndNames() {
        return productService.findIdAndNames();
    }

    @GetMapping("/measurementunits/all")
    public List<IdNameAndUnit> returnIdAndMU() {
        return productService.productMeasurementUnit();
    }

    @GetMapping
    public List<IdNameAndUnit> getProducts(
            @RequestParam(name = "isManagedInventory", required = false) Boolean isManagedInventory,
            @RequestParam(name = "categoryId", required = false) Long categoryId
    ) {
        return productService.getProducts(isManagedInventory, categoryId);
    }

    @GetMapping("/typeahead/{name}")
    public List<String> getTypeAhead(@PathVariable String name) {
        return productService.typeAheadDataList(name);
    }

    @GetMapping("/categorynames")
    public List<IdNameProjections> getCategoryNamesforDropdown() {
        return productService.getIdAndNamesForCategoryDropdown();
    }

    /** GET tenant-specific reorder level config for a product */
    @GetMapping("/{id}/tenant-config")
    public ResponseEntity<ProductTenantConfigDTO> getTenantConfig(@PathVariable Long id) throws Exception {
        Product product = productService.findSingleProduct(id);
        return ResponseEntity.ok(
                productTenantConfigService.getConfig(id, product.getReorderQuantity())
        );
    }

    /** PUT — save or update tenant-specific reorder level override */
    @PutMapping("/{id}/tenant-config")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public ResponseEntity<ProductTenantConfigDTO> saveTenantConfig(
            @PathVariable Long id,
            @RequestBody ProductTenantConfigDTO payload) throws Exception {
        Product product = productService.findSingleProduct(id);
        return ResponseEntity.ok(
                productTenantConfigService.saveOverride(id, payload.getOverrideReorderLevel(), product.getReorderQuantity())
        );
    }

    /** DELETE — remove tenant-specific reorder level override, revert to global */
    @DeleteMapping("/{id}/tenant-config")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public ResponseEntity<Void> removeTenantConfig(@PathVariable Long id) {
        productTenantConfigService.removeOverride(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
