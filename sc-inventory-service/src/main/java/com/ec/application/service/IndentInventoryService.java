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
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.util.LineItemCodeGenerator;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    Logger log = LoggerFactory.getLogger(IndentInventoryService.class);

    @Transactional(rollbackFor = Exception.class)
    public IndentInventory createIndentInventory(IndentInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        IndentInventory indentInventory = new IndentInventory();
        validateInputsForCreate(iiData);

        // Set basic fields (without inventory list)
        indentInventory.setTenant(tenantService.fetchTenantFromHeader());
        indentInventory.setTenantSchemaCode(schemaConfig.getSchemaCode(ThreadLocalStorage.getTenantName()));
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        indentInventory.setIndentDate(iiData.getIndentDate());
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_NEW);

        // First save to generate indentId
        indentInventoryRepo.save(indentInventory);
        indentInventoryRepo.flush(); // Ensure ID is generated

        // Now add inventory list with proper line item codes
        Set<IndentInventoryList> inventoryList = processInventoryListForCreation(
                iiData.getInventoryList(),
                indentInventory
        );
        indentInventory.setInventoryList(inventoryList);

        // Save again with inventory list
        indentInventoryRepo.save(indentInventory);

        // Force load the list before returning (to avoid lazy init exception)
        indentInventory.getInventoryList().size();
        if (iiData.getDraftId() != null)
            draftService.deleteDraft(iiData.getDraftId());
        indentInventoryUiEnricher.enrich(indentInventory);
        return indentInventory;
    }

    /**
     * Process inventory list for CREATION
     * Generates unique line item codes for each product
     */
    private Set<IndentInventoryList> processInventoryListForCreation(
            List<IndentProductDTO> indentProductDTOs,
            IndentInventory indentInventory) {

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
            String lineItemCode = LineItemCodeGenerator.generateInitialCode(
                    indentInventory.getIndentId(),
                    String.valueOf(product.getProductId())
            );
            item.setLineItemCode(lineItemCode);
            item.setParentLineItemCode(null); // No parent for initial items

            inventoryList.add(item);
        }

        return inventoryList;
    }

    /**
     * Process inventory list for UPDATE
     * Handles existing items, new items, and split items
     */
    private Set<IndentInventoryList> processInventoryListForUpdate(
            List<IndentProductDTO> indentProductDTOs,
            IndentInventory indentInventory,  // CHANGED: Accept IndentInventory instead of separate params
            Set<IndentInventoryList> existingInventoryList) {

        Set<IndentInventoryList> processedList = new HashSet<>();

        for (IndentProductDTO dto : indentProductDTOs) {
            IndentInventoryList item = new IndentInventoryList();

            // SET THE PARENT REFERENCE FOR NEW ITEMS - THIS IS CRITICAL!
            item.setIndentInventory(indentInventory);

            // Set product
            Product product = productRepo.findByProductId(dto.getProductId());
            item.setProduct(product);

            // Set other fields
            item.setQuantity(dto.getQuantity());
            item.setRemarks(dto.getRemarks());
            item.setSpecification(dto.getSpecification());
            item.setMeasurementUnit(product.getMeasurementUnit());
            item.setLineItemStatus(item.getLineItemStatus());  // FIXED: was item.getLineItemStatus()

            // Handle line item code
            if (dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty()) {
                // Existing item being updated - keep the same code
                item.setLineItemCode(dto.getLineItemCode());
                item.setParentLineItemCode(dto.getParentLineItemCode());

            } else if (dto.getParentLineItemCode() != null && !dto.getParentLineItemCode().isEmpty()) {
                // New split item - generate split code
                int splitIndex = LineItemCodeGenerator.getNextSplitIndex(
                        dto.getParentLineItemCode(),
                        existingInventoryList
                );
                String splitCode = LineItemCodeGenerator.generateSplitCode(
                        dto.getParentLineItemCode(),
                        splitIndex
                );
                item.setLineItemCode(splitCode);
                item.setParentLineItemCode(dto.getParentLineItemCode());

            } else {
                // Brand new product being added - generate initial code
                String lineItemCode = LineItemCodeGenerator.generateInitialCode(
                        indentInventory.getIndentId(),
                        String.valueOf(product.getProductId())
                );
                item.setLineItemCode(lineItemCode);
                item.setParentLineItemCode(null);
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
                // Item exists - update mutable fields only
                existingItem.setProduct(newItem.getProduct());
                existingItem.setQuantity(newItem.getQuantity());
                existingItem.setSpecification(newItem.getSpecification());
                existingItem.setRemarks(newItem.getRemarks());
                existingItem.setMeasurementUnit(newItem.getMeasurementUnit());
                existingItem.setLineItemStatus(existingItem.getLineItemStatus());

                itemsToKeep.add(existingItem);
            } else {
                // New item (brand new or split) - add to collection
                indentInventory.getInventoryList().add(newItem);
                itemsToKeep.add(newItem);
            }
        }

        // Remove items that are no longer in the new list
        indentInventory.getInventoryList().removeIf(item -> !itemsToKeep.contains(item));
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
        //validate for duplicate line item codes
        Long duplicateLineItemCodeCount = iiData.getInventoryList().stream()
                .filter(dto -> dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty())
                .collect(Collectors.groupingBy(IndentProductDTO::getLineItemCode, counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateLineItemCodeCount > 0)
            throw new Exception("Duplicate line item codes found. Each line item must have a unique code.");
    }

    private void basicValidation(IndentInventoryData iiData, String x) throws Exception {

        if (iiData.getIndentDate() == null)
            throw new Exception("Indent Date is a mandatory field");

        for (IndentProductDTO dto : iiData.getInventoryList()) {
            Optional<Product> productOpt = productRepo.findById(dto.getProductId());

            if (!productOpt.isPresent())
                throw new Exception("Product not found with ID " + dto.getProductId());
            else if (productOpt.get().getIsManagedInventory() == false)
                throw new Exception("Product with ID " + dto.getProductId() + x);
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
    public ReturnIndentInventoryData fetchIndentInventory(FilterDataList filterDataList, Pageable pageable) throws ParseException {

        ReturnIndentInventoryData returnData = new ReturnIndentInventoryData();
        Specification<IndentInventory> spec = IndentInventorySpecification.getSpecification(filterDataList);
        Specification<IndentInventory> specWithTenant = IndentInventorySpecification.getTenantSpecification(tenantService.fetchTenantFromHeader(), spec);
        Page<IndentInventory> page = (spec != null) ? indentInventoryRepo.findAll(specWithTenant, pageable) : indentInventoryRepo.findAll(pageable);
        // Enrich ONCE for UI
        indentInventoryUiEnricher.enrich(page.getContent());
        returnData.setIndentInventories(page);
        returnData.setIiDropdown(populateDropdownService.fetchData("indent"));
        return returnData;
    }

    public IndentInventory findById(String id) throws Exception {
        IndentInventory indentInventory = indentInventoryRepo.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new RuntimeException("Indent Inventory not found with ID " + id));
        indentInventoryUiEnricher.enrich(indentInventory);
        return indentInventory;
    }

    public void deleteInwardInventoryById(String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        String action = indentValidationService.validateBeforeDelete(indentInventory);
        if (action.equalsIgnoreCase("DELETE")) {
            indentInventoryRepo.softDelete(indentInventory);
        }
        if (action.equalsIgnoreCase("CANCEL")) {
            indentInventory.setIndentStatus(IndentStatusConstants.STATUS_CANCELLED);
            indentInventoryRepo.save(indentInventory);
        }
    }


    /**
     * Updated UPDATE method with synchronization logic
     */
    public IndentInventory updateIndentInventory(IndentInventoryData payload, String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        indentValidationService.validateBeforeUpdate(indentInventory);
        validateInputsForUpdate(payload);
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));
        indentInventory.setIndentDate(payload.getIndentDate());

        // Process and synchronize inventory list
        Set<IndentInventoryList> processedInventoryList = processInventoryListForUpdate(
                payload.getInventoryList(),
                indentInventory,  // CHANGED: Pass the object instead of ID
                indentInventory.getInventoryList()
        );

        syncInventoryList(indentInventory, processedInventoryList);
        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }

    public IndentInventory validateAndGetIndentInventoryForModification(String id) {
        Optional<IndentInventory> indentInventoryOptional = indentInventoryRepo.findByIdWithDetails(id);
        if (!indentInventoryOptional.isPresent()) {
            throw new RuntimeException("Indent Inventory not found with ID " + id);
        }
        return indentInventoryOptional.get();
    }

    /**
     * Split a line item into TWO items
     * Only quantity and code change, everything else stays the same
     */
    @Transactional(rollbackFor = Exception.class)
    public IndentInventory splitLineItem(String indentId, SplitLineItemRequest request) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(indentId);

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
        return indentInventory;
    }

    public IndentInventory approveIndentInventory(String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        indentValidationService.validateBeforeApprove(indentInventory);
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_APPROVED);
        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }
}