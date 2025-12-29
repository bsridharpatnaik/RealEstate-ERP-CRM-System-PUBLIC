package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.config.IndentLineItemStatusConstants;
import com.ec.application.config.IndentStatusConstants;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.util.LineItemCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class IndentInventoryService {

    @Autowired
    IndentInventoryRepo indentInventoryRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(IndentInventoryService.class);

    @Transactional(rollbackFor = Exception.class)
    public IndentInventory createIndentInventory(IndentInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        IndentInventory indentInventory = new IndentInventory();
        validateInputsForCreate(iiData);

        // Set basic fields (without inventory list)
        indentInventory.setTenantSchemaCode(schemaConfig.getSchemaCode(ThreadLocalStorage.getTenantName()));
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        indentInventory.setIndentDate(iiData.getIndentDate());
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_CREATED);

        // First save to generate indentId
        indentInventoryRepo.save(indentInventory);
        indentInventoryRepo.flush(); // Ensure ID is generated

        // Now add inventory list with proper line item codes
        List<IndentInventoryList> inventoryList = processInventoryListForCreation(
                iiData.getInventoryList(),
                indentInventory  // PASS THE PARENT OBJECT
        );
        indentInventory.setInventoryList(inventoryList);

        // Save again with inventory list
        indentInventoryRepo.save(indentInventory);

        return indentInventory;
    }

    /**
     * Process inventory list for CREATION
     * Generates unique line item codes for each product
     */
    private List<IndentInventoryList> processInventoryListForCreation(
            List<IndentProductDTO> indentProductDTOs,
            IndentInventory indentInventory) {  // CHANGED: Accept IndentInventory instead of String

        List<IndentInventoryList> inventoryList = new ArrayList<>();

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
            item.setLineItemStatus(IndentLineItemStatusConstants.STATUS_CREATED);

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
    private List<IndentInventoryList> processInventoryListForUpdate(
            List<IndentProductDTO> indentProductDTOs,
            IndentInventory indentInventory,  // CHANGED: Accept IndentInventory instead of separate params
            List<IndentInventoryList> existingInventoryList) {

        List<IndentInventoryList> processedList = new ArrayList<>();

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
    private void syncInventoryList(IndentInventory indentInventory, List<IndentInventoryList> newInventoryList) {
        if (newInventoryList == null) {
            newInventoryList = new ArrayList<>();
        }

        if (indentInventory.getInventoryList() == null) {
            indentInventory.setInventoryList(new ArrayList<>());
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
                existingItem.setLineItemStatus(newItem.getLineItemStatus());

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
        if (iiData.getIndentDate() == null)
            throw new Exception("Indent Date is a mandatory field");

        for (IndentProductDTO dto : iiData.getInventoryList()) {
            Optional<Product> productOpt = productRepo.findById(dto.getProductId());

            if (!productOpt.isPresent())
                throw new Exception("Product not found with ID " + dto.getProductId());
            else if (productOpt.get().getIsManagedInventory() == false)
                throw new Exception("Product with ID " + dto.getProductId() + " is not managed inventory. Cannot be added to Indent Inventory.");
            if (dto.getQuantity() <= 0)
                throw new Exception("Quantity cannot be less than or equal to zero");
        }

        // During creation, same product cannot appear multiple times
        Long duplicateProductIdCount = iiData.getInventoryList().stream()
                .collect(Collectors.groupingBy(IndentProductDTO::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Same product cannot be added multiple times during creation. Use split functionality after creation if needed.");
    }

    /**
     * Validation for UPDATE - allows duplicate products (due to split functionality)
     */
    private void validateInputsForUpdate(IndentInventoryData iiData) throws Exception {
        if (iiData.getIndentDate() == null)
            throw new Exception("Indent Date is a mandatory field");

        for (IndentProductDTO dto : iiData.getInventoryList()) {
            Optional<Product> productOpt = productRepo.findById(dto.getProductId());

            if (!productOpt.isPresent())
                throw new Exception("Product not found with ID " + dto.getProductId());
            else if (productOpt.get().getIsManagedInventory() == false)
                throw new Exception("Product with ID " + dto.getProductId() + " is not managed inventory.");
            if (dto.getQuantity() <= 0)
                throw new Exception("Quantity cannot be less than or equal to zero");
        }

        // No duplicate check here - same product can appear multiple times due to split
        // But validate that line item codes are unique
        Long duplicateLineItemCodeCount = iiData.getInventoryList().stream()
                .filter(dto -> dto.getLineItemCode() != null && !dto.getLineItemCode().isEmpty())
                .collect(Collectors.groupingBy(IndentProductDTO::getLineItemCode, counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateLineItemCodeCount > 0)
            throw new Exception("Duplicate line item codes found. Each line item must have a unique code.");
    }

    public ReturnIndentInventoryData fetchIndentInventory(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        ReturnIndentInventoryData returnData = new ReturnIndentInventoryData();
        Specification<IndentInventory> spec = IndentInventorySpecification.getSpecification(filterDataList);

        if (spec != null)
            returnData.setIndentInventories(indentInventoryRepo.findAll(spec, pageable));
        else
            returnData.setIndentInventories(indentInventoryRepo.findAll(pageable));

        return returnData;
    }

    public IndentInventory findById(String id) {
        return indentInventoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Indent Inventory not found with ID " + id));
    }

    public void deleteInwardInventoryById(String id) {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        indentInventoryRepo.softDelete(indentInventory);
    }

    /**
     * Updated UPDATE method with synchronization logic
     */
    public IndentInventory updateInwardnventory(IndentInventoryData payload, String id) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(id);
        validateInputsForUpdate(payload);

        // Update file informations
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));

        // Update indent date
        indentInventory.setIndentDate(payload.getIndentDate());

        // Process and synchronize inventory list
        List<IndentInventoryList> processedInventoryList = processInventoryListForUpdate(
                payload.getInventoryList(),
                indentInventory,  // CHANGED: Pass the object instead of ID
                indentInventory.getInventoryList()
        );

        syncInventoryList(indentInventory, processedInventoryList);

        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }

    public IndentInventory validateAndGetIndentInventoryForModification(String id) {
        Optional<IndentInventory> indentInventoryOptional = indentInventoryRepo.findById(id);
        if (!indentInventoryOptional.isPresent()) {
            throw new RuntimeException("Indent Inventory not found with ID " + id);
        }

        IndentInventory indentInventory = indentInventoryOptional.get();
        if (!indentInventory.getIndentStatus().equalsIgnoreCase(IndentStatusConstants.STATUS_CREATED))
            throw new RuntimeException("Indent Inventory cannot be edited after PO is created.");

        return indentInventory;
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

        // Find next available split indices
        String parentCode = request.getLineItemCode();
        int splitIndex1 = LineItemCodeGenerator.getNextSplitIndex(parentCode, indentInventory.getInventoryList());
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

        String splitCode1 = LineItemCodeGenerator.generateSplitCode(parentCode, splitIndex1);
        splitItem1.setLineItemCode(splitCode1);
        splitItem1.setParentLineItemCode(parentCode);

        // Create second split item (with remainder quantity)
        IndentInventoryList splitItem2 = new IndentInventoryList();
        splitItem2.setIndentInventory(indentInventory);  // SET PARENT!
        splitItem2.setProduct(originalItem.getProduct());
        splitItem2.setQuantity(remainderQuantity);
        splitItem2.setSpecification(originalItem.getSpecification());
        splitItem2.setRemarks(originalItem.getRemarks());
        splitItem2.setMeasurementUnit(originalItem.getMeasurementUnit());
        splitItem2.setLineItemStatus(originalItem.getLineItemStatus());

        String splitCode2 = LineItemCodeGenerator.generateSplitCode(parentCode, splitIndex2);
        splitItem2.setLineItemCode(splitCode2);
        splitItem2.setParentLineItemCode(parentCode);

        // Add new split items to inventory list
        indentInventory.getInventoryList().add(splitItem1);
        indentInventory.getInventoryList().add(splitItem2);

        // Remove the original item from the collection
        indentInventory.getInventoryList().remove(originalItem);

        // Save changes
        indentInventoryRepo.save(indentInventory);

        log.info("Split line item {} (qty: {}) into {} (qty: {}) and {} (qty: {})",
                parentCode, originalItem.getQuantity(),
                splitCode1, request.getSplitQuantity(),
                splitCode2, remainderQuantity);

        return indentInventory;
    }
}