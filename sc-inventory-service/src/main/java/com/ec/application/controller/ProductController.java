package com.ec.application.controller;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.multipart.MultipartFile;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.IdNameAndUnit;
import com.ec.application.data.AllTenantReorderConfigDTO;
import com.ec.application.data.ProductCreateData;
import com.ec.application.data.TenantReorderSaveRequest;
import com.ec.application.data.ProductUnitConversionDTO;
import com.ec.application.model.Product;
import com.ec.application.model.ProductUnitConversion;
import com.ec.application.service.ProductService;
import com.ec.application.service.ProductTenantConfigService;
import com.ec.application.service.ProductUnitConversionService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    ProductService productService;

    @Autowired
    ProductTenantConfigService productTenantConfigService;

    @Autowired
    ProductUnitConversionService productUnitConversionService;

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
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) throws Exception {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public Product createProduct(@RequestBody ProductCreateData payload) throws Exception {
        return productService.createProduct(payload);
    }

    @PutMapping("/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
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

    /**
     * GET all tenants' reorder-level configs for a product.
     * No tenant-id header needed — loops all tenant schemas internally.
     * Response: List<AllTenantReorderConfigDTO>
     */
    @GetMapping("/{id}/all-tenant-reorder-configs")
    public ResponseEntity<List<AllTenantReorderConfigDTO>> getAllTenantReorderConfigs(
            @PathVariable Long id) throws Exception {
        Product product = productService.findSingleProduct(id);
        return ResponseEntity.ok(
                productTenantConfigService.getAllTenantConfigs(id, product.getReorderQuantity())
        );
    }

    /**
     * PUT — save (upsert) reorder-level override for one specific tenant.
     * Body: { "tenantName": "drgtrdcntr", "reorderLevel": 50.0 }
     */
    @PutMapping("/{id}/tenant-reorder-config")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<Void> saveTenantReorderConfig(
            @PathVariable Long id,
            @RequestBody TenantReorderSaveRequest request) {
        productTenantConfigService.saveOverrideForTenant(id, request.getTenantName(), request.getReorderLevel());
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE — remove reorder-level override for one specific tenant.
     * After deletion the tenant falls back to the global Product.reorderQuantity.
     */
    @DeleteMapping("/{id}/tenant-reorder-config/{tenantName}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<Void> removeTenantReorderConfig(
            @PathVariable Long id,
            @PathVariable String tenantName) {
        productTenantConfigService.removeOverrideForTenant(id, tenantName);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /product/import — upload an Excel file to bulk-update products.
     * Only productName, reorderLevel, managedInventory, batchMode are updated.
     * Returns { updated, skipped, errors }.
     */
    @PostMapping("/import")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<?> importProducts(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message", "No file uploaded."));
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!filename.endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message",
                            "Invalid file format. Please upload an .xlsx file (Excel). Received: "
                            + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")));
        }
        try {
            Map<String, Object> result = productService.importProducts(file);
            return ResponseEntity.ok(result);
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message",
                            "File could not be read as Excel. Make sure you upload a valid .xlsx file."));
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message",
                            "Could not read file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Collections.singletonMap("message",
                            "Import failed: " + e.getMessage()));
        }
    }

    // ── Unit Conversion endpoints ──────────────────────────────────────────────

    @GetMapping("/{id}/unit-conversions")
    public List<ProductUnitConversionDTO> getUnitConversions(@PathVariable Long id) {
        return productUnitConversionService.getConversions(id);
    }

    @PostMapping("/{id}/unit-conversions")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ProductUnitConversion addUnitConversion(
            @PathVariable Long id,
            @RequestBody ProductUnitConversion body) {
        return productUnitConversionService.addConversion(id, body.getUnitName(), body.getConversionFactor(),
                body.getDisplayDirection(), body.getReferenceUnit(), body.getReferenceValue());
    }

    @PutMapping("/{id}/unit-conversions/{conversionId}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ProductUnitConversion updateUnitConversion(
            @PathVariable Long id,
            @PathVariable Long conversionId,
            @RequestBody ProductUnitConversion body) {
        return productUnitConversionService.updateConversion(conversionId, body.getUnitName(),
                body.getConversionFactor(), body.getDisplayDirection(), body.getReferenceUnit(),
                body.getReferenceValue());
    }

    @DeleteMapping("/{id}/unit-conversions/{conversionId}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<Void> deleteUnitConversion(
            @PathVariable Long id,
            @PathVariable Long conversionId) {
        productUnitConversionService.deleteConversion(conversionId);
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
