package com.ec.application.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.config.SchemaConfig;
import com.ec.application.multitenant.ThreadLocalStorage;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import javax.transaction.Transactional;

import com.ec.application.ReusableClasses.ActivityLogDescription;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.BatchMode;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.InventoryMonthPriceMappingRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    SchemaConfig schemaConfig;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    InventoryNotificationService inService;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    InventoryMonthPriceMappingRepository inventoryMonthPriceMappingRepository;

    @Autowired
    ActivityLogService activityLogService;

    Logger log = LoggerFactory.getLogger(ProductService.class);

    public Page<Product> findAll(Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return productRepo.findAll(pageable);
    }

    @Caching(evict = {
        @CacheEvict(value = "refProducts",   allEntries = true),
        @CacheEvict(value = "refCategories", allEntries = true)
    })
    public Product createProduct(ProductCreateData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        checkIfDashboardProductLimitReached(null, payload, "create");
        if (!productRepo.existsByProductName(payload.getProductName().trim())) {
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
        product.setBatchMode(resolveBatchMode(payload));
        product.setLeadTimeDays(payload.getLeadTimeDays());
        product.setDefaultExpiryDays(payload.getDefaultExpiryDays());
        return product;
    }

    /** Resolves batchMode from payload. New field takes priority; falls back to legacy isExpirable. */
    private static BatchMode resolveBatchMode(ProductCreateData payload) {
        if (payload.getBatchMode() != null) return payload.getBatchMode();
        // Legacy fallback: isExpirable=true → BATCH_WITH_EXPIRY
        return Boolean.TRUE.equals(payload.getIsExpirable()) ? BatchMode.BATCH_WITH_EXPIRY : BatchMode.NONE;
    }

    private void checkIfDashboardProductLimitReached(Product productForUpdate, ProductCreateData payload, String action) throws Exception {

        if (action.equals("create") && payload.getShowOnDashboard()) {
            List<Product> existingDashboardProducts = productRepo.getDashboardProducts();
            if (existingDashboardProducts.size() >= ProjectConstants.noOfProductsForDashboard)
                throw new Exception("Only " + ProjectConstants.noOfProductsForDashboard + " products can be shown in dashboard. Please uncheck flag Show In Dashboard");
        } else if (action.equals("update")) {
            List<Product> existingDashboardProducts = productRepo.getDashboardProducts();
            if (payload.getShowOnDashboard() && !existingDashboardProducts.contains(productForUpdate) && existingDashboardProducts.size() >= ProjectConstants.noOfProductsForDashboard)
                throw new Exception("Only " + ProjectConstants.noOfProductsForDashboard + " products can be shown in dashboard. Please uncheck flag Show In Dashboard");
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

        if (payload.getProductName().contains(","))
            throw new Exception("Comma(,) not allowed in product name. Please enter valid product name.");

        if (payload.getReorderQuantity() == null || payload.getReorderQuantity() == 0)
            throw new Exception("Reorder Quantity cannot be zero or empty. Please Enter Reorder Quantity");

        if (payload.getProductName().length() > 50)
            throw new Exception("Product Name should not exceed 50 characters. Please provide valid product name.");
    }

    @Caching(evict = {
        @CacheEvict(value = "refProducts",   allEntries = true),
        @CacheEvict(value = "refCategories", allEntries = true)
    })
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

        product.setProductName(payload.getProductName().trim());
        product.setProductDescription(payload.getProductDescription() == null ? ""
                : payload.getProductDescription().trim());
        product.setMeasurementUnit(payload.getMeasurementUnit() == null ? ""
                : payload.getMeasurementUnit().trim());
        product.setCategory(category);
        product.setReorderQuantity(payload.getReorderQuantity());
        product.setShowOnDashboard(Boolean.TRUE.equals(payload.getShowOnDashboard()));
        product.setIsManagedInventory(payload.getIsManagedInventory() == null || payload.getIsManagedInventory());
        BatchMode newBatchMode = resolveBatchMode(payload);
        BatchMode existingBatchMode = product.getBatchMode() != null ? product.getBatchMode() : BatchMode.NONE;
        checkBatchModeChangeAllowed(product.getProductId(), existingBatchMode, newBatchMode);
        product.setBatchMode(newBatchMode);
        product.setLeadTimeDays(payload.getLeadTimeDays());
        product.setDefaultExpiryDays(payload.getDefaultExpiryDays());

        Product saved = productRepo.save(product);

        try {
            String user = resolveCurrentUser();
            String desc = "{\"summary\": \"Product '" + saved.getProductName() + "' updated by " + user
                    + "\", \"items\": [{\"field\": \"Batch Tracking\", \"oldValue\": \"" + existingBatchMode
                    + "\", \"newValue\": \"" + newBatchMode + "\"}]}";
            activityLogService.record("UPDATED", "PRODUCT", String.valueOf(saved.getProductId()), desc, user);
        } catch (Exception e) {
            log.warn("Failed to record activity log for product update: {}", e.getMessage());
        }

        return saved;
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
            throw new Exception("Product is a global entity and cannot be deleted. Please contact administrator for further assistance.");
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

    @Cacheable(value = "refCategories", key = "'all'")
    public List<IdNameProjections> getIdAndNamesForCategoryDropdown() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<IdNameProjections> categoryNamesForDropdown = categoryRepo.findIdAndNames();
        return categoryNamesForDropdown;
    }

    public List<IdNameAndUnit> productMeasurementUnit() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return productRepo.getProductMeasurementUnit();
    }

    @Cacheable(value = "refProducts", key = "#isManagedInventory + ':' + #categoryId")
    public List<IdNameAndUnit> getProducts(Boolean isManagedInventory, Long categoryId) {
        return productRepo.getProducts(isManagedInventory, categoryId);
    }

    /**
     * Bulk-import products from Excel.
     * Columns (0-indexed): 0=Product Name, 1=Product Code, 2=Description,
     *   3=Reorder Level, 4=Measurement Unit, 5=Category, 6=Managed Inventory,
     *   7=Can Expire, 8=Batch Tracking, 9=Lead Time (Days), 10=Default Expiry (Days)
     * Only productName, reorderQuantity, isManagedInventory, batchMode, leadTimeDays,
     * defaultExpiryDays are updated.
     * One activity log entry per changed product.
     */
    @Caching(evict = {
        @CacheEvict(value = "refProducts",   allEntries = true),
        @CacheEvict(value = "refCategories", allEntries = true)
    })
    public Map<String, Object> importProducts(MultipartFile file) throws IOException {
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<String> updatedItems = new ArrayList<>();
        String currentUser = resolveCurrentUser();

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // Validate header row — must have at least Product Name and Product Code
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("File appears to be empty — no header row found.");
            }
            String col0Header = getCellString(headerRow, 0);
            String col1Header = getCellString(headerRow, 1);
            if (!"Product Name".equalsIgnoreCase(col0Header) || !"Product Code".equalsIgnoreCase(col1Header)) {
                throw new IOException(
                    "Unexpected column headers. Expected 'Product Name' in column A and 'Product Code' in column B. "
                    + "Found: '" + col0Header + "' and '" + col1Header + "'. "
                    + "Please use the file downloaded from this page.");
            }

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) { skipped++; continue; }

                String productName  = getCellString(row, 0);
                String productCode  = getCellString(row, 1);
                String reorderRaw   = getCellString(row, 3);
                String managedRaw   = getCellString(row, 6);
                String batchModeRaw   = getCellString(row, 8);
                String leadTimeRaw    = getCellString(row, 9);
                String defaultExpiryRaw = getCellString(row, 10);

                // Need at least one identifier
                if ((productCode == null || productCode.isEmpty()) &&
                    (productName  == null || productName.isEmpty())) {
                    skipped++;
                    continue;
                }

                // Find product: by code first, fallback to name
                Product product = null;
                if (productCode != null && !productCode.isEmpty()) {
                    List<Product> byCode = productRepo.findByproductCode(productCode);
                    if (!byCode.isEmpty()) product = byCode.get(0);
                }
                if (product == null && productName != null && !productName.isEmpty()) {
                    product = productRepo.findByProductName(productName);
                }
                if (product == null) {
                    errors.add("Row " + (i + 1) + ": Product not found (code='" + productCode + "', name='" + productName + "')");
                    skipped++;
                    continue;
                }

                List<String> changes = new ArrayList<>();
                boolean rowError = false;

                // --- productName ---
                if (productName != null && !productName.isEmpty() &&
                        !productName.equalsIgnoreCase(product.getProductName())) {
                    if (productRepo.existsByProductName(productName)) {
                        errors.add("Row " + (i + 1) + ": Product name '" + productName + "' already taken by another product");
                        skipped++;
                        rowError = true;
                    } else {
                        changes.add("productName: '" + product.getProductName() + "'→'" + productName.trim() + "'");
                        product.setProductName(productName.trim());
                    }
                }
                if (rowError) continue;

                // --- reorderQuantity ---
                if (reorderRaw != null && !reorderRaw.isEmpty()) {
                    try {
                        double reorderLevel = Double.parseDouble(reorderRaw.trim());
                        if (reorderLevel < 0) {
                            errors.add("Row " + (i + 1) + ": Reorder level cannot be negative");
                            skipped++;
                            continue;
                        }
                        Double oldReorder = product.getReorderQuantity();
                        if (oldReorder == null || Math.abs(oldReorder - reorderLevel) > 0.0001) {
                            changes.add("reorderLevel: " + oldReorder + "→" + reorderLevel);
                            product.setReorderQuantity(reorderLevel);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (i + 1) + ": Invalid reorder level '" + reorderRaw + "'");
                        skipped++;
                        continue;
                    }
                }

                // --- isManagedInventory ---
                if (managedRaw != null && !managedRaw.isEmpty()) {
                    boolean newManaged = "yes".equalsIgnoreCase(managedRaw.trim());
                    boolean oldManaged = product.getIsManagedInventory() == null || product.getIsManagedInventory();
                    if (oldManaged != newManaged) {
                        changes.add("managedInventory: " + (oldManaged ? "Yes" : "No") + "→" + (newManaged ? "Yes" : "No"));
                        product.setIsManagedInventory(newManaged);
                    }
                }

                // --- batchMode ---
                if (batchModeRaw != null && !batchModeRaw.isEmpty()) {
                    BatchMode newBatchMode;
                    try {
                        newBatchMode = BatchMode.valueOf(batchModeRaw.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add("Row " + (i + 1) + ": Invalid Batch Tracking value '" + batchModeRaw
                                + "'. Valid: NONE, BATCH_ONLY, BATCH_WITH_EXPIRY");
                        skipped++;
                        continue;
                    }
                    BatchMode oldBatchMode = product.getBatchMode() != null ? product.getBatchMode() : BatchMode.NONE;
                    if (oldBatchMode != newBatchMode) {
                        try {
                            checkBatchModeChangeAllowed(product.getProductId(), oldBatchMode, newBatchMode);
                        } catch (Exception ex) {
                            errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                            skipped++;
                            continue;
                        }
                        changes.add("batchMode: " + oldBatchMode + "→" + newBatchMode);
                        product.setBatchMode(newBatchMode);
                    }
                }

                // --- leadTimeDays ---
                if (leadTimeRaw != null && !leadTimeRaw.isEmpty()) {
                    try {
                        int ltVal = Integer.parseInt(leadTimeRaw.trim());
                        if (ltVal < 0) {
                            errors.add("Row " + (i + 1) + ": Lead time cannot be negative");
                            skipped++;
                            continue;
                        }
                        Integer oldLt = product.getLeadTimeDays();
                        if (oldLt == null || oldLt != ltVal) {
                            changes.add("leadTimeDays: " + oldLt + "→" + ltVal);
                            product.setLeadTimeDays(ltVal);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (i + 1) + ": Invalid lead time '" + leadTimeRaw + "'");
                        skipped++;
                        continue;
                    }
                }

                // --- defaultExpiryDays ---
                if (defaultExpiryRaw != null && !defaultExpiryRaw.isEmpty()) {
                    try {
                        int deVal = Integer.parseInt(defaultExpiryRaw.trim());
                        if (deVal < 0) {
                            errors.add("Row " + (i + 1) + ": Default Expiry (Days) cannot be negative");
                            skipped++;
                            continue;
                        }
                        Integer oldDe = product.getDefaultExpiryDays();
                        if (oldDe == null || oldDe != deVal) {
                            changes.add("defaultExpiryDays: " + oldDe + "→" + deVal);
                            product.setDefaultExpiryDays(deVal);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + (i + 1) + ": Invalid Default Expiry (Days) '" + defaultExpiryRaw + "'");
                        skipped++;
                        continue;
                    }
                }

                if (!changes.isEmpty()) {
                    try {
                        productRepo.save(product);
                        String summary = "Product '" + product.getProductName() + "' updated via bulk import by "
                                + currentUser + ": " + String.join(", ", changes);
                        activityLogService.record("UPDATED", "PRODUCT",
                                String.valueOf(product.getProductId()),
                                ActivityLogDescription.of(summary), currentUser);
                        updatedItems.add("[" + product.getProductCode() + "] " + product.getProductName()
                                + " — " + String.join(", ", changes));
                        updated++;
                    } catch (Exception e) {
                        errors.add("Row " + (i + 1) + ": Failed to save: " + e.getMessage());
                        skipped++;
                    }
                } else {
                    skipped++; // no changes — nothing to do
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("updatedItems", updatedItems);
        result.put("errors", errors);
        return result;
    }

    /**
     * Blocks batch mode changes when the product has live stock in any tenant schema.
     * Iterates all non-master schemas, temporarily switching ThreadLocal context for each query.
     */
    private void checkBatchModeChangeAllowed(Long productId, BatchMode existingMode, BatchMode newMode) throws Exception {
        if (existingMode == newMode) return;
        // Switching FROM None → any batch mode is always allowed.
        // Switching FROM a batch mode (to None or between batch modes) is blocked if batches exist.
        if (existingMode == BatchMode.NONE) return;
        for (String schema : schemaConfig.getNonMasterSchemaList()) {
            Number batchCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM `" + schema + "`.inventory_batch WHERE product_id = :pid AND is_deleted = 0")
                .setParameter("pid", productId)
                .getSingleResult();
            if (batchCount.longValue() > 0) {
                throw new Exception(
                    "Cannot change batch tracking mode: this product has " + batchCount.longValue() +
                    " batch record(s) in project '" + schema + "'. " +
                    "Remove all batch records before changing the tracking mode."
                );
            }
        }
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return "";
        }
    }

    List<Product> getDashboardProducts() {
        int requiredCount = ProjectConstants.noOfProductsForDashboard;

        // 1. Fetch flagged products
        List<Product> flaggedProducts = productRepo.getDashboardProducts();

        // If we already have enough, return exactly requiredCount
        if (flaggedProducts.size() >= requiredCount) {
            return flaggedProducts.subList(0, requiredCount);
        }

        // 2. Fetch remaining products excluding already selected ones
        Set<Long> selectedIds = flaggedProducts.stream()
                .map(Product::getProductId)
                .collect(Collectors.toSet());

        List<Product> remainingProducts = productRepo.findAll().stream()
                .filter(p -> !selectedIds.contains(p.getProductId()))
                .collect(Collectors.toList());

        // 3. Shuffle to make selection random
        Collections.shuffle(remainingProducts);

        // 4. Pick only what is needed
        int remainingNeeded = requiredCount - flaggedProducts.size();
        List<Product> randomFill = remainingProducts.stream()
                .limit(remainingNeeded)
                .collect(Collectors.toList());

        // 5. Merge and return
        List<Product> dashboardProducts = new ArrayList<>(flaggedProducts);
        dashboardProducts.addAll(randomFill);
        return dashboardProducts;
    }

}
