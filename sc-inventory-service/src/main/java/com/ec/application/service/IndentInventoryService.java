package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.*;
import com.ec.application.enricher.IndentInventoryUiEnricher;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.indentpo.IndentCompletionEvaluator;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.util.LineItemCodeGenerator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;

@Service
@Transactional
@UseDefaultTenant
public class IndentInventoryService {

    @Autowired
    IndentInventoryRepo indentInventoryRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    SchemaConfig schemaConfig;

    @Autowired
    DraftService draftService;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    IndentValidationService indentValidationService;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    IndentInventoryUiEnricher indentInventoryUiEnricher;

    @Autowired
    TenantService tenantService;

    @Autowired
    DeadStockService deadStockService;

    @Autowired
    IndentStatusHistoryService indentStatusHistoryService;

    @Autowired
    IndentInventoryListRepo indentInventoryListRepo;

    @Autowired
    IndentCompletionEvaluator indentCompletionEvaluator;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    LeadTimeResolver leadTimeResolver;

    List<String> indentPOEligibleStatuses = Arrays.asList(
            IndentStatusConstants.STATUS_APPROVED,
            IndentStatusConstants.STATUS_PO_PARTIAL,
            IndentStatusConstants.STATUS_INWARD_PARTIAL
    );
    List<String> indentLineItemPoEligibleStatuses = Arrays.asList(IndentLineItemStatusConstants.STATUS_NEW);

    Logger log = LoggerFactory.getLogger(IndentInventoryService.class);

    @Transactional(rollbackFor = Exception.class)
    public IndentInventory createIndentInventory(IndentInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        IndentInventory indentInventory = new IndentInventory();
        validateInputsForCreate(iiData);
        // Set basic fields (without inventory list)
        String tenantName = tenantService.fetchTenantFromHeader();
        exitIfReadOnly(tenantName);
        if (tenantName == null)
            throw new IllegalStateException("No request context available to fetch tenant-id.");

        indentInventory.setTenant(tenantName);
        indentInventory.setTenantSchemaCode(schemaConfig.getSchemaCode(tenantService.fetchTenantFromHeader()));
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        indentInventory.setIndentDate(iiData.getIndentDate());
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_NEW);
        indentInventory.setLastStatusUpdatedAt(new Date());
        // First save to generate indentId
        indentInventoryRepo.save(indentInventory);
        indentInventoryRepo.flush(); // Ensure ID is generated

        // Now add inventory list with proper line item codes
        Set<IndentInventoryList> inventoryList = processInventoryListForCreation(iiData.getInventoryList(), indentInventory);
        indentInventory.setInventoryList(inventoryList);

        // Save again with inventory list
        indentInventoryRepo.save(indentInventory);

        // Force load the list before returning (to avoid lazy init exception)
        indentInventory.getInventoryList().size();
        indentStatusHistoryService.logStatusChange(indentInventory, null, IndentStatusConstants.STATUS_NEW, userDetailsService.getCurrentUser().getUsername(), "Indent created by " + userDetailsService.getCurrentUser().getUsername(), null);
        draftService.deleteDraftForUser("INDENT");
        indentInventoryUiEnricher.enrich(indentInventory);
        activityLogService.record("CREATED", "INDENT", indentInventory.getIndentId(),
                "Indent " + indentInventory.getIndentId() + " created with " + indentInventory.getInventoryList().size() + " line item(s)",
                resolveCurrentUser());
        return indentInventory;
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }

    private void exitIfReadOnly(String tenantName) throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();
        boolean readOnly = true;
        for (UserTenantMapping ut : currentUser.getTenantList()) {
            if (ut.getTenant().getName().equalsIgnoreCase(tenantName)) {
                if (ut.getAuthorization().equals(AuthorizationEnum.FullAccess)) {
                    readOnly = false;
                    break;
                }
            }
        }

        if (readOnly)
            throw new Exception("User not allowed to add/modify data for this project");
    }

    /**
     * Process inventory list for CREATION
     * Generates unique line item codes for each product
     */
    private Set<IndentInventoryList> processInventoryListForCreation(List<IndentProductDTO> indentProductDTOs, IndentInventory indentInventory) {
        Set<IndentInventoryList> inventoryList = new HashSet<>();

        for (IndentProductDTO dto : indentProductDTOs) {
            IndentInventoryList item = new IndentInventoryList();

            // SET THE PARENT REFERENCE - THIS IS CRITICAL!
            item.setIndentInventory(indentInventory);

            // Set product
            Product product = productRepo.findByProductId(dto.getProductId());
            item.setProduct(product);

            // Set other fields
            item.setQuantity(dto.getQuantity());
            item.setRemarks(dto.getRemarks());
            item.setSpecification(dto.getSpecification());
            item.setMeasurementUnit(product.getMeasurementUnit());
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);

            // Generate unique line item code: INDENT_ID/PRODUCT_ID
            String lineItemCode = LineItemCodeGenerator.generateInitialCode(indentInventory.getIndentId(), String.valueOf(product.getProductId()));
            item.setLineItemCode(lineItemCode);
            item.setParentLineItemCode(null); // No parent for initial items
            inventoryList.add(item);
        }
        return inventoryList;
    }

    /**
     * Process inventory list for UPDATE
     * - Existing items: keep lineItemCode & status
     * - Split items: generate split code, status = NEW
     * - New products: generate initial code, status = NEW
     */
    private Set<IndentInventoryList> processInventoryListForUpdate(List<IndentProductDTO> indentProductDTOs, IndentInventory indentInventory, Set<IndentInventoryList> existingInventoryList) {
        Set<IndentInventoryList> processedList = new HashSet<>();

        for (IndentProductDTO dto : indentProductDTOs) {
            IndentInventoryList item = new IndentInventoryList();
            // Always set parent
            item.setIndentInventory(indentInventory);
            // Product
            Product product = productRepo.findByProductId(dto.getProductId());
            item.setProduct(product);

            // Mutable fields (always allowed)
            item.setQuantity(dto.getQuantity());
            item.setRemarks(dto.getRemarks());
            item.setSpecification(dto.getSpecification());
            item.setMeasurementUnit(product.getMeasurementUnit());
            /*
             * CASE 1: Existing line item (update)
             * - Keep same lineItemCode
             * - DO NOT touch status (DB value must survive)
             */
            if (dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty()) {
                item.setLineItemCode(dto.getLineItemCode());
                item.setParentLineItemCode(dto.getParentLineItemCode());
                // IMPORTANT: status intentionally NOT set here
            }

            /*
             * CASE 2: Split item
             * - Generate split code under root parent
             * - Status must be NEW
             */
            else if (dto.getParentLineItemCode() != null && !dto.getParentLineItemCode().isEmpty()) {
                int splitIndex = LineItemCodeGenerator.getNextSplitIndex(dto.getParentLineItemCode(), existingInventoryList);
                String splitCode = LineItemCodeGenerator.generateSplitCode(dto.getParentLineItemCode(), splitIndex);
                item.setLineItemCode(splitCode);
                item.setParentLineItemCode(dto.getParentLineItemCode());
                item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
            }

            /*
             * CASE 3: Brand new product added during update
             * - Generate initial lineItemCode
             * - Status must be NEW
             */
            else {
                String lineItemCode = LineItemCodeGenerator.generateInitialCode(
                        indentInventory.getIndentId(),
                        String.valueOf(product.getProductId())
                );
                item.setLineItemCode(lineItemCode);
                item.setParentLineItemCode(null);
                item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_NEW);
            }
            processedList.add(item);
        }
        return processedList;
    }


    /**
     * Synchronize inventory list during update
     * Only updates changed items, adds new items, removes deleted items
     */
    private void syncInventoryList(IndentInventory indentInventory, Set<IndentInventoryList> newInventoryList) {
        if (newInventoryList == null) {
            newInventoryList = new HashSet<>();
        }

        if (indentInventory.getInventoryList() == null) {
            indentInventory.setInventoryList(new HashSet<>());
        }

        // Create a map of existing items by lineItemCode
        Map<String, IndentInventoryList> existingItemsMap = indentInventory.getInventoryList().stream()
                .collect(Collectors.toMap(
                        IndentInventoryList::getLineItemCode,
                        item -> item
                ));

        // Track which items should remain
        Set<IndentInventoryList> itemsToKeep = new HashSet<>();

        // Process each new item
        for (IndentInventoryList newItem : newInventoryList) {
            String lineItemCode = newItem.getLineItemCode();
            IndentInventoryList existingItem = existingItemsMap.get(lineItemCode);

            if (existingItem != null) {
                // Non-NEW line items are locked — skip silently, keep as-is
                if (!IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(existingItem.getLineItemStatus())) {
                    itemsToKeep.add(existingItem);
                    continue;
                }
                // Item exists and is NEW — update mutable fields
                existingItem.setProduct(newItem.getProduct());
                existingItem.setQuantity(newItem.getQuantity());
                existingItem.setSpecification(newItem.getSpecification());
                existingItem.setRemarks(newItem.getRemarks());
                existingItem.setMeasurementUnit(newItem.getMeasurementUnit());
                itemsToKeep.add(existingItem);
            } else {
                // New item (brand new or split) - add to collection
                indentInventory.getInventoryList().add(newItem);
                itemsToKeep.add(newItem);
            }
        }
        // Remove items that are no longer in the new list
        for (IndentInventoryList existingItem : indentInventory.getInventoryList()) {
            if (!itemsToKeep.contains(existingItem)) {

                // Only NEW line items can be removed
                if (!IndentLineItemStatusConstants.STATUS_NEW.equalsIgnoreCase(existingItem.getLineItemStatus())) {
                    throw new RuntimeException("Cannot remove line item '" + existingItem.getLineItemCode()
                            + "' — it is in status: " + existingItem.getLineItemStatus());
                }

                existingItem.setDeleted(true);   // ✅ SOFT DELETE
            }
        }
    }

    /**
     * Validation for CREATE - no duplicate products allowed
     */
    private void validateInputsForCreate(IndentInventoryData iiData) throws Exception {
        basicValidation(iiData, " is not managed inventory. Cannot be added to Indent Inventory.");
    }

    /**
     * Validation for UPDATE - allows duplicate products (due to split functionality)
     */
    private void validateInputsForUpdate(IndentInventoryData iiData) throws Exception {

        basicValidation(iiData, " is not managed inventory.");

        // 1️⃣ Duplicate lineItemCode check (already correct)
        long duplicateLineItemCodeCount =
                iiData.getInventoryList().stream()
                        .filter(dto -> dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty())
                        .collect(Collectors.groupingBy(IndentProductDTO::getLineItemCode, counting()))
                        .values().stream()
                        .filter(count -> count > 1)
                        .count();

        if (duplicateLineItemCodeCount > 0) {
            throw new Exception("Duplicate line item codes found.");
        }

        // 2️⃣ Enforce product duplication ONLY via split
        Map<Long, Set<String>> productToRoots = new HashMap<>();

        for (IndentProductDTO dto : iiData.getInventoryList()) {

            String root;

            if (dto.getParentLineItemCode() != null && !dto.getParentLineItemCode().isEmpty()) {
                root = dto.getParentLineItemCode();
            } else if (dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty()) {
                root = dto.getLineItemCode();
            } else {
                root = "NEW"; // brand-new product attempt
            }

            productToRoots
                    .computeIfAbsent(dto.getProductId(), k -> new HashSet<>())
                    .add(root);
        }

        // If same product maps to multiple roots → illegal duplication
        for (Map.Entry<Long, Set<String>> entry : productToRoots.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new Exception("Product ID " + entry.getKey() + " can appear multiple times only via split from the same line item.");
            }
        }
    }


    private void basicValidation(IndentInventoryData iiData, String x) throws Exception {

        if (iiData.getIndentDate() == null)
            throw new Exception("Indent Date is a mandatory field");

        for (IndentProductDTO dto : iiData.getInventoryList()) {
            Optional<Product> productOpt = productRepo.findById(dto.getProductId());

            if (!productOpt.isPresent())
                throw new Exception("Product not found with ID " + dto.getProductId());
            if (dto.getQuantity() <= 0)
                throw new Exception("Quantity cannot be less than or equal to zero");
        }

        // During creation, same product cannot appear multiple times
        Long duplicateProductIdCount = iiData.getInventoryList().stream()
                .collect(Collectors.groupingBy(IndentProductDTO::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Same product cannot be added multiple times during creation/updation. Use split functionality if needed.");
    }

    @Transactional(readOnly = true)
    public ReturnIndentInventoryData fetchIndentInventory(
            FilterDataList filterDataList,
            Pageable pageable) throws Exception {

        ReturnIndentInventoryData returnData = new ReturnIndentInventoryData();

        // 🔐 Single secured spec builder
        Specification<IndentInventory> spec =
                IndentInventorySpecification.buildSpecificationWithTenantSecurity(
                        filterDataList,
                        tenantService.fetchTenantFromHeader(),
                        userDetailsService.getCurrentUser().getAllowedTenants()
                );

        // STEP 1: Page only parent IDs
        Page<IndentInventory> idPage =
                indentInventoryRepo.findAll(spec, pageable);

        List<String> ids = idPage.getContent()
                .stream()
                .map(IndentInventory::getIndentId)
                .collect(Collectors.toList());

        // STEP 2: Fetch full graph safely
        List<IndentInventory> full =
                ids.isEmpty()
                        ? Collections.emptyList()
                        : indentInventoryRepo.findWithDetailsByIndentIdIn(ids);

        Map<String, IndentInventory> map =
                full.stream()
                        .collect(Collectors.toMap(
                                IndentInventory::getIndentId,
                                i -> i
                        ));

        List<IndentInventory> ordered =
                ids.stream()
                        .map(map::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        Page<IndentInventory> page =
                new PageImpl<>(ordered, pageable, idPage.getTotalElements());

        // Enrich UI data
        indentInventoryUiEnricher.enrich(page.getContent());

        returnData.setIndentInventories(page);
        returnData.setIiDropdown(populateDropdownService.fetchData("indent"));

        return returnData;
    }

    public IndentInventory findById(String id) throws Exception {
        IndentInventory indentInventory = indentInventoryRepo.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new RuntimeException("Indent Inventory not found with ID " + id));
        String tenant = indentInventory.getTenant();
        exitIfTenantNotAllowed(tenant);
        indentInventoryUiEnricher.enrich(indentInventory);
        return indentInventory;
    }

    private void exitIfTenantNotAllowed(String tenant) throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();
        if (!currentUser.getAllowedTenants().contains(tenant)) {
            throw new Exception("User not allowed to access data for Project: " + tenant);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void deleteInwardInventoryById(String id, String remarks) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        exitIfReadOnly(indentInventory.getTenant());
        String action = indentValidationService.validateBeforeDelete(indentInventory);
        String currentUser = userDetailsService.getCurrentUser().getUsername();

        if (action.equalsIgnoreCase("CANCEL")) {
            indentStatusHistoryService.logStatusChange(
                    indentInventory,
                    indentInventory.getIndentStatus(),
                    IndentStatusConstants.STATUS_CANCELLED,
                    currentUser,
                    "Indent cancelled by " + currentUser,
                    null);
            indentInventory.setIndentStatus(IndentStatusConstants.STATUS_CANCELLED);
            indentInventory.setLastStatusUpdatedAt(new Date());
            indentInventoryRepo.save(indentInventory);
            activityLogService.record("CANCELLED", "INDENT", id,
                    "Indent " + id + " cancelled by " + currentUser, currentUser);

        } else if (action.equalsIgnoreCase("REJECT")) {
            String message = "Indent rejected by " + currentUser;
            if (remarks != null && !remarks.trim().isEmpty()) {
                message += ". Reason: " + remarks;
            }
            indentStatusHistoryService.logStatusChange(
                    indentInventory,
                    indentInventory.getIndentStatus(),
                    IndentStatusConstants.STATUS_REJECTED,
                    currentUser,
                    message,
                    null);
            indentInventory.setIndentStatus(IndentStatusConstants.STATUS_REJECTED);
            indentInventory.setLastStatusUpdatedAt(new Date());
            indentInventoryRepo.save(indentInventory);
            activityLogService.record("DELETED", "INDENT", id,
                    "Indent " + id + " rejected by " + currentUser, currentUser);
        }
    }


    /**
     * Updated UPDATE method with synchronization logic
     */
    @Transactional(rollbackFor = Exception.class)
    public IndentInventory updateIndentInventory(IndentInventoryData payload, String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        exitIfReadOnly(indentInventory.getTenant());
        indentValidationService.validateBeforeUpdate(indentInventory);
        validateInputsForUpdate(payload);

        // ── Capture before-state for history ──────────────────────────────
        Date oldIndentDate = indentInventory.getIndentDate();
        Map<String, Double> beforeQty     = new HashMap<>();
        Map<String, String> beforeProduct = new HashMap<>();
        Map<String, String> beforeRemarks = new HashMap<>();
        Map<String, String> beforeSpec    = new HashMap<>();
        for (IndentInventoryList item : indentInventory.getInventoryList()) {
            if (!item.isDeleted()) {
                String code = item.getLineItemCode();
                beforeQty.put(code, item.getQuantity());
                beforeProduct.put(code, item.getProduct().getProductName());
                beforeRemarks.put(code, item.getRemarks());
                beforeSpec.put(code, item.getSpecification());
            }
        }
        // ──────────────────────────────────────────────────────────────────

        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));
        indentInventory.setIndentDate(payload.getIndentDate());

        // Process and synchronize inventory list
        Set<IndentInventoryList> processedInventoryList = processInventoryListForUpdate(
                payload.getInventoryList(),
                indentInventory,
                indentInventory.getInventoryList()
        );

        syncInventoryList(indentInventory, processedInventoryList);

        // ── Log edit history ───────────────────────────────────────────────
        String changeMessage = buildEditChangeMessage(
                oldIndentDate, payload.getIndentDate(),
                beforeQty, beforeProduct, beforeRemarks, beforeSpec,
                indentInventory.getInventoryList());
        indentStatusHistoryService.logStatusChange(
                indentInventory,
                indentInventory.getIndentStatus(),
                indentInventory.getIndentStatus(),
                userDetailsService.getCurrentUser().getUsername(),
                changeMessage,
                null);
        // ──────────────────────────────────────────────────────────────────

        indentInventoryRepo.save(indentInventory);
        activityLogService.record("UPDATED", "INDENT", id,
                "Indent " + id + " updated by " + resolveCurrentUser(), resolveCurrentUser());
        return indentInventory;
    }

    public IndentInventory validateAndGetIndentInventoryForModification(String id) throws Exception {
        Optional<IndentInventory> indentInventoryOptional = indentInventoryRepo.findByIdWithDetails(id);
        if (!indentInventoryOptional.isPresent()) {
            throw new RuntimeException("Indent Inventory not found with ID " + id);
        }
        exitIfTenantNotAllowed(indentInventoryOptional.get().getTenant());
        return indentInventoryOptional.get();
    }

    /**
     * Split a line item into TWO items
     * Only quantity and code change, everything else stays the same
     */
    @Transactional(rollbackFor = Exception.class)
    public IndentInventory splitLineItem(String indentId, SplitLineItemRequest request) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(indentId);
        exitIfReadOnly(indentInventory.getTenant());
        // Find the original item to be split
        IndentInventoryList originalItem = indentInventory.getInventoryList().stream()
                .filter(item -> item.getLineItemCode().equals(request.getLineItemCode()))
                .findFirst()
                .orElseThrow(() -> new Exception("Line item not found with code: " + request.getLineItemCode()));

        indentValidationService.validateBeforeSplit(indentInventory, originalItem);

        // Validate split quantity
        if (request.getSplitQuantity() == null || request.getSplitQuantity() <= 0) {
            throw new Exception("Split quantity must be greater than zero");
        }

        if (request.getSplitQuantity() >= originalItem.getQuantity()) {
            throw new Exception("Split quantity (" + request.getSplitQuantity() +
                    ") must be less than original quantity (" + originalItem.getQuantity() + ")");
        }

        // Calculate remainder quantity
        Double remainderQuantity = originalItem.getQuantity() - request.getSplitQuantity();

        // Determine ROOT parent line item code
        String rootParentCode =
                originalItem.getParentLineItemCode() != null
                        ? originalItem.getParentLineItemCode()
                        : originalItem.getLineItemCode();

        // Find next available split index under ROOT parent
        int splitIndex1 = LineItemCodeGenerator.getNextSplitIndex(
                rootParentCode,
                indentInventory.getInventoryList()
        );
        int splitIndex2 = splitIndex1 + 1;

        // Create first split item (with requested quantity)
        IndentInventoryList splitItem1 = new IndentInventoryList();
        splitItem1.setIndentInventory(indentInventory);  // SET PARENT!
        splitItem1.setProduct(originalItem.getProduct());
        splitItem1.setQuantity(request.getSplitQuantity());
        splitItem1.setSpecification(originalItem.getSpecification());
        splitItem1.setRemarks(originalItem.getRemarks());
        splitItem1.setMeasurementUnit(originalItem.getMeasurementUnit());
        splitItem1.setLineItemStatus(originalItem.getLineItemStatus());

        String splitCode1 = LineItemCodeGenerator.generateSplitCode(rootParentCode, splitIndex1);
        splitItem1.setLineItemCode(splitCode1);
        splitItem1.setParentLineItemCode(rootParentCode);

        // Create second split item (with remainder quantity)
        IndentInventoryList splitItem2 = new IndentInventoryList();
        splitItem2.setIndentInventory(indentInventory);  // SET PARENT!
        splitItem2.setProduct(originalItem.getProduct());
        splitItem2.setQuantity(remainderQuantity);
        splitItem2.setSpecification(originalItem.getSpecification());
        splitItem2.setRemarks(originalItem.getRemarks());
        splitItem2.setMeasurementUnit(originalItem.getMeasurementUnit());
        splitItem2.setLineItemStatus(originalItem.getLineItemStatus());

        String splitCode2 = LineItemCodeGenerator.generateSplitCode(rootParentCode, splitIndex2);
        splitItem2.setLineItemCode(splitCode2);
        splitItem2.setParentLineItemCode(rootParentCode);

        // Add new split items to inventory list
        indentInventory.getInventoryList().add(splitItem1);
        indentInventory.getInventoryList().add(splitItem2);

        // Remove the original item from the collection
        //indentInventory.getInventoryList().remove(originalItem);
        originalItem.setDeleted(true);
        // Save changes
        indentInventoryRepo.save(indentInventory);
        activityLogService.record("SPLIT", "INDENT", indentId,
                "Indent " + indentId + " line item split by " + resolveCurrentUser(), resolveCurrentUser());
        return indentInventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public IndentInventory approveIndentInventory(String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        exitIfReadOnly(indentInventory.getTenant());
        indentValidationService.validateBeforeApprove(indentInventory);
        indentStatusHistoryService.logStatusChange(indentInventory, indentInventory.getIndentStatus(), IndentStatusConstants.STATUS_APPROVED, userDetailsService.getCurrentUser().getUsername(), "Indent approved by " + userDetailsService.getCurrentUser().getUsername(), null);
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_APPROVED);
        indentInventory.setLastStatusUpdatedAt(new Date());
        indentInventoryRepo.save(indentInventory);
        activityLogService.record("APPROVED", "INDENT", id,
                "Indent " + id + " approved by " + resolveCurrentUser(), resolveCurrentUser());
        return indentInventory;
    }

    /**
     * Fetch all PO-eligible indent line items
     * grouped by category across all tenants.
     */
    public Map<String, List<ConsolidatedIndentLineDTO>> fetchGroupedByCategory(String sortBy, Sort.Direction direction) {
        List<IndentInventory> inventoryList = indentInventoryRepo.findByIndentStatusIn(indentPOEligibleStatuses);
        List<ConsolidatedIndentLineDTO> allLines = new ArrayList<>(flatten(inventoryList));

        // Apply sorting
        Comparator<ConsolidatedIndentLineDTO> comparator = buildComparator(sortBy, direction);

        if (comparator != null) {
            allLines.sort(comparator);
        }

        // Group by category
        return allLines.stream()
                .collect(Collectors.groupingBy(
                        ConsolidatedIndentLineDTO::getCategoryName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Comparator<ConsolidatedIndentLineDTO> buildComparator(String sortBy, Sort.Direction direction) {

        if (sortBy == null) {
            sortBy = "productName";
        }

        if (direction == null) {
            direction = Sort.Direction.ASC;
        }

        Comparator<ConsolidatedIndentLineDTO> comparator;

        switch (sortBy) {
            case "productName":
                comparator = Comparator.comparing(
                        ConsolidatedIndentLineDTO::getProductName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;

            case "indentNo":
                comparator = Comparator.comparing(
                        ConsolidatedIndentLineDTO::getIndentNo,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;

            case "creationDate":
                comparator = Comparator.comparing(
                        ConsolidatedIndentLineDTO::getCreationDate,
                        Comparator.nullsLast(Date::compareTo)
                );
                break;

            case "quantity":
                comparator = Comparator.comparing(
                        ConsolidatedIndentLineDTO::getQuantity,
                        Comparator.nullsLast(Double::compareTo)
                );
                break;

            default:
                return null; // unknown field → no sort
        }

        return direction == Sort.Direction.DESC
                ? comparator.reversed()
                : comparator;
    }


    List<ConsolidatedIndentLineDTO> flatten(List<IndentInventory> indents) {

        List<ConsolidatedIndentLineDTO> result = new ArrayList<>();

        // 1. Collect productIds only once (and only eligible ones)
        Set<Long> productIds = new HashSet<>();

        for (IndentInventory indent : indents) {
            for (IndentInventoryList line : indent.getInventoryList()) {
                if (indentLineItemPoEligibleStatuses.contains(line.getLineItemStatus())) {
                    productIds.add(line.getProduct().getProductId());
                }
            }
        }

        // 2. Fetch dead stock in one go
        Map<Long, DeadStockDTOForIndent> deadStocks = deadStockService.fetchDeadStockForProductIds(new ArrayList<>(productIds));
        Map<Long, CurrentStockDTOForIndent> currentStocks = deadStockService.fetchCurrentStockForProductIds(new ArrayList<>(productIds));

        // 3. Flatten
        for (IndentInventory indent : indents) {
            String tenant = indent.getTenant();
            String tenantCode = schemaConfig.getSchemaMap().get(tenant);
            for (IndentInventoryList line : indent.getInventoryList()) {
                if (!indentLineItemPoEligibleStatuses.contains(line.getLineItemStatus()))
                    continue;
                Product p = line.getProduct();
                Category c = p.getCategory();
                DeadStockDTOForIndent deadStock = deadStocks.getOrDefault(p.getProductId(), new DeadStockDTOForIndent(0.0, Collections.emptyList()));
                CurrentStockDTOForIndent currentStock = currentStocks.getOrDefault(p.getProductId(), new CurrentStockDTOForIndent(0.0, Collections.emptyList()));
                ConsolidatedIndentLineDTO dto = new ConsolidatedIndentLineDTO(tenant, tenantCode, indent.getIndentDate(), indent.getIndentId(), line.getLineItemCode(), c.getCategoryName(), p.getProductId(), p.getProductName(), p.getMeasurementUnit(), line.getQuantity(), line.getSpecification(), line.getRemarks(), line.getLineItemStatus(), indent.getCreationDate(), deadStock, currentStock, null);
                dto.setLeadTimeDays(leadTimeResolver.resolve(p));
                result.add(dto);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, DashboardChartDTO> getCurrentIndentDashboards() {

        List<String> statuses = Arrays.asList(
                IndentStatusConstants.STATUS_NEW,            // Awaiting Approval
                IndentStatusConstants.STATUS_APPROVED,       // No PO
                IndentStatusConstants.STATUS_PO_PARTIAL,     // PO Partial
                IndentStatusConstants.STATUS_INWARD_PARTIAL  // Inward Partial
        );

        List<StatusGroupCountDTO> rows = indentInventoryRepo.fetchCurrentIndentStatusCounts(statuses);

        Map<String, Map<String, Long>> grouped = new HashMap<>();

        for (StatusGroupCountDTO row : rows) {
            grouped
                    .computeIfAbsent(row.getStatus(), k -> new HashMap<>())
                    .merge(row.getGroupKey(), row.getCount(), Long::sum);
        }
        Map<String, DashboardChartDTO> dashboards = new HashMap<>();
        for (String status : statuses) {
            Map<String, Long> tenantMap = grouped.getOrDefault(status, new HashMap<>());

            List<TenantCountDTO> tenantCounts = new ArrayList<>();
            long total = 0;

            for (Map.Entry<String, Long> e : tenantMap.entrySet()) {
                tenantCounts.add(new TenantCountDTO(e.getKey(), e.getValue()));
                total += e.getValue();
            }
            dashboards.put(status, new DashboardChartDTO(total, tenantCounts));
        }
        return dashboards;
    }

    @UseDefaultTenant
    public void streamIndentExcel(
            FilterDataList filterDataList,
            OutputStream os) throws Exception {

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Indents");

        int rowNum = 0;

        // Header
        Row header = sheet.createRow(rowNum++);
        String[] columns = {
                "Indent No",
                "Tenant",
                "Indent Date",
                "Indent Status",
                "Last Status Updated At",
                "Line Item Code",
                "Product",
                "Category",
                "Quantity Requested",
                "Quantity Received",
                "Quantity Pending",
                "Line Item Status",
                "Specification",
                "Remarks",
                "PO Number"
        };

        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        // 🔐 Same secured spec builder as UI
        Specification<IndentInventory> spec =
                IndentInventorySpecification.buildSpecificationWithTenantSecurity(
                        filterDataList,
                        tenantService.fetchTenantFromHeader(),
                        userDetailsService.getCurrentUser().getAllowedTenants()
                );

        int page = 0;
        int size = 500;
        Page<IndentInventory> result;

        do {
            Pageable pageable = PageRequest.of(page, size);

            result = indentInventoryRepo.findAll(spec, pageable);

            rowNum = writeExcelPage(
                    result.getContent(),
                    sheet,
                    rowNum
            );

            page++;

        } while (!result.isLast());

        workbook.write(os);
        workbook.dispose(); // IMPORTANT for SXSSFWorkbook
    }

    private int writeExcelPage(
            List<IndentInventory> indents,
            Sheet sheet,
            int rowNum) {

        for (IndentInventory indent : indents) {
            for (IndentInventoryList line : indent.getInventoryList()) {

                Row row = sheet.createRow(rowNum++);
                int col = 0;

                // ===== Indent-level fields =====
                row.createCell(col++).setCellValue(indent.getIndentId());
                row.createCell(col++).setCellValue(indent.getTenant());
                row.createCell(col++).setCellValue(
                        indent.getIndentDate() != null
                                ? indent.getIndentDate().toString()
                                : ""
                );
                row.createCell(col++).setCellValue(
                        safeExcel(indent.getIndentStatus())
                );
                row.createCell(col++).setCellValue(
                        indent.getLastStatusUpdatedAt() != null
                                ? indent.getLastStatusUpdatedAt().toString()
                                : ""
                );

                // ===== Line-item-level fields =====
                row.createCell(col++).setCellValue(
                        safeExcel(line.getLineItemCode())
                );
                row.createCell(col++).setCellValue(
                        safeExcel(line.getProduct().getProductName())
                );
                row.createCell(col++).setCellValue(
                        safeExcel(line.getProduct().getCategory().getCategoryName())
                );

                // Quantities
                double qty = line.getQuantity() != null ? line.getQuantity() : 0.0;
                double qtyReceived = line.getQuantityReceived() != null ? line.getQuantityReceived() : 0.0;
                double qtyPending = line.getQuantityPending() != null
                        ? line.getQuantityPending()
                        : qty - qtyReceived;
                row.createCell(col++).setCellValue(qty);
                row.createCell(col++).setCellValue(qtyReceived);
                row.createCell(col++).setCellValue(qtyPending);

                // Status, specification & remarks
                row.createCell(col++).setCellValue(
                        safeExcel(line.getLineItemStatus())
                );
                row.createCell(col++).setCellValue(
                        safeExcel(line.getSpecification())
                );
                row.createCell(col++).setCellValue(
                        safeExcel(line.getRemarks())
                );
                row.createCell(col++).setCellValue(
                        safeExcel(line.getPurchaseOrderId())
                );
            }
        }
        return rowNum;
    }

    private String safeExcel(String value) {
        if (value == null) return "";
        if (value.startsWith("=") || value.startsWith("+")
                || value.startsWith("-") || value.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }

    private String buildEditChangeMessage(
            Date oldDate, Date newDate,
            Map<String, Double> beforeQty,
            Map<String, String> beforeProduct,
            Map<String, String> beforeRemarks,
            Map<String, String> beforeSpec,
            Set<IndentInventoryList> currentItems) {

        List<String> changes = new ArrayList<>();

        if (oldDate != null && newDate != null && !oldDate.equals(newDate)) {
            changes.add("Indent date changed from " + oldDate + " to " + newDate);
        }

        Set<String> beforeCodes = beforeQty.keySet();
        Set<String> afterCodes = currentItems.stream()
                .filter(i -> !i.isDeleted())
                .map(IndentInventoryList::getLineItemCode)
                .collect(Collectors.toSet());

        // Added items
        for (IndentInventoryList item : currentItems) {
            if (!item.isDeleted() && !beforeCodes.contains(item.getLineItemCode())) {
                changes.add("Added: " + item.getProduct().getProductName()
                        + " (Qty: " + item.getQuantity() + " " + item.getMeasurementUnit() + ")");
            }
        }

        // Removed items
        for (String code : beforeCodes) {
            if (!afterCodes.contains(code)) {
                changes.add("Removed: " + beforeProduct.get(code) + " [" + code + "]");
            }
        }

        // Modified items
        for (IndentInventoryList item : currentItems) {
            String code = item.getLineItemCode();
            if (!item.isDeleted() && beforeCodes.contains(code)) {
                List<String> fieldChanges = new ArrayList<>();
                if (!Objects.equals(beforeQty.get(code), item.getQuantity())) {
                    fieldChanges.add("qty: " + beforeQty.get(code) + " → " + item.getQuantity());
                }
                if (!Objects.equals(beforeRemarks.get(code), item.getRemarks())) {
                    fieldChanges.add("remarks updated");
                }
                if (!Objects.equals(beforeSpec.get(code), item.getSpecification())) {
                    fieldChanges.add("specification updated");
                }
                if (!fieldChanges.isEmpty()) {
                    changes.add("Updated: " + item.getProduct().getProductName()
                            + " [" + code + "] — " + String.join(", ", fieldChanges));
                }
            }
        }

        return changes.isEmpty()
                ? "Indent edited (no field changes detected)"
                : "Indent edited — " + String.join("; ", changes);
    }

    @Transactional(rollbackFor = Exception.class)
    public IndentInventory cancelLineItem(String indentId, String lineItemCode) throws Exception {
        IndentInventory indent = indentInventoryRepo.findById(indentId)
                .orElseThrow(() -> new Exception("Indent not found: " + indentId));
        IndentInventoryList line = indent.getInventoryList().stream()
                .filter(li -> li.getLineItemCode().equals(lineItemCode))
                .findFirst()
                .orElseThrow(() -> new Exception("Line item not found: " + lineItemCode));
        if (!IndentLineItemStatusConstants.CANCEL_ALLOWED_STATUSES.contains(line.getLineItemStatus())) {
            throw new Exception("Line item cannot be cancelled in status: " + line.getLineItemStatus());
        }
        String prevLineStatus = line.getLineItemStatus();
        line.setLineItemStatus(IndentLineItemStatusConstants.STATUS_CANCELLED);
        indentInventoryListRepo.save(line);
        indentStatusHistoryService.logStatusChange(indent, null, null,
                userDetailsService.getCurrentUser().getUsername(),
                "Line item " + lineItemCode + " cancelled by user.", null);

        boolean allCancelled = indent.getInventoryList().stream()
                .allMatch(li -> IndentLineItemStatusConstants.STATUS_CANCELLED.equals(li.getLineItemStatus()));
        if (allCancelled) {
            String oldIndentStatus = indent.getIndentStatus();
            indent.setIndentStatus(IndentStatusConstants.STATUS_CANCELLED);
            indent.setLastStatusUpdatedAt(new Date());
            indentStatusHistoryService.logStatusChange(indent, oldIndentStatus, IndentStatusConstants.STATUS_CANCELLED,
                    userDetailsService.getCurrentUser().getUsername(),
                    "Indent auto-cancelled as all line items were cancelled.", null);
            indentInventoryRepo.save(indent);
        } else {
            indentCompletionEvaluator.evaluate(indent);
        }
        return indentInventoryRepo.findById(indentId).get();
    }
}