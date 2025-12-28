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
        setFieldsForCreate(indentInventory, iiData);
        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }

    /**
     * Set fields during CREATION
     */
    private void setFieldsForCreate(IndentInventory indentInventory, IndentInventoryData iiData) {
        indentInventory.setTenantSchemaCode(schemaConfig.getSchemaCode(ThreadLocalStorage.getTenantName()));
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        indentInventory.setIndentDate(iiData.getIndentDate());
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_CREATED);

        // Generate line item codes for new inventory items
        // Note: indentId will be generated after save, so we need to handle this
        // Option 1: Generate indentId before setting inventory list
        // Option 2: Set inventory list after save in a separate step

        // For now, we'll set a temporary list and update after save
        indentInventory.setInventoryList(new ArrayList<>());
    }

    /**
     * Process inventory list after indent is created (has indentId)
     */
    @Transactional(rollbackFor = Exception.class)
    public void setInitialInventoryList(IndentInventory indentInventory, IndentInventoryData iiData) {
        List<IndentInventoryList> inventoryList = processInventoryListForCreation(
                iiData.getInventoryList(),
                indentInventory.getIndentId()
        );
        indentInventory.setInventoryList(inventoryList);
        indentInventoryRepo.save(indentInventory);
    }

    /**
     * Process inventory list for CREATION
     * Generates unique line item codes for each product
     */
    private List<IndentInventoryList> processInventoryListForCreation(
            List<IndentProductDTO> indentProductDTOs,
            String indentId) {

        List<IndentInventoryList> inventoryList = new ArrayList<>();

        for (IndentProductDTO dto : indentProductDTOs) {
            IndentInventoryList item = new IndentInventoryList();

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
                    indentId,
                    String.valueOf(dto.getProductId())
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
            String indentId,
            List<IndentInventoryList> existingInventoryList) {

        List<IndentInventoryList> processedList = new ArrayList<>();

        for (IndentProductDTO dto : indentProductDTOs) {
            IndentInventoryList item = new IndentInventoryList();

            // Set product
            Product product = productRepo.findByProductId(dto.getProductId());
            item.setProduct(product);

            // Set other fields
            item.setQuantity(dto.getQuantity());
            item.setRemarks(dto.getRemarks());
            item.setSpecification(dto.getSpecification());
            item.setMeasurementUnit(product.getMeasurementUnit());
            item.setLineItemStatus(item.getLineItemStatus());

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
                        indentId,
                        String.valueOf(dto.getProductId())
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
        // This removes from collection, which deletes the mapping from join table
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
                indentInventory.getIndentId(),
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
     * Split a line item into multiple items
     */
    @Transactional(rollbackFor = Exception.class)
    public IndentInventory splitLineItem(String indentId, String lineItemCode, List<IndentProductDTO> splitItemsData) throws Exception {
        IndentInventory indentInventory = validateAndGetIndentInventoryForModification(indentId);

        // Find the original item to be split
        IndentInventoryList originalItem = indentInventory.getInventoryList().stream()
                .filter(item -> item.getLineItemCode().equals(lineItemCode))
                .findFirst()
                .orElseThrow(() -> new Exception("Line item not found with code: " + lineItemCode));

        // Validate total quantity matches
        double totalSplitQuantity = splitItemsData.stream()
                .mapToDouble(IndentProductDTO::getQuantity)
                .sum();

        if (Math.abs(totalSplitQuantity - originalItem.getQuantity()) > 0.01) {
            throw new Exception("Total quantity of split items (" + totalSplitQuantity +
                    ") must equal original quantity (" + originalItem.getQuantity() + ")");
        }

        // Create split items
        List<IndentInventoryList> newSplitItems = new ArrayList<>();
        int splitIndex = 1;

        for (IndentProductDTO splitDto : splitItemsData) {
            // Find next available split index
            int nextIndex = LineItemCodeGenerator.getNextSplitIndex(
                    lineItemCode,
                    indentInventory.getInventoryList()
            );

            IndentInventoryList splitItem = new IndentInventoryList();
            splitItem.setProduct(originalItem.getProduct());
            splitItem.setQuantity(splitDto.getQuantity());
            splitItem.setSpecification(splitDto.getSpecification());
            splitItem.setRemarks(splitDto.getRemarks());
            splitItem.setMeasurementUnit(originalItem.getMeasurementUnit());
            splitItem.setLineItemStatus(splitItem.getLineItemStatus());

            // Generate split code
            String splitCode = LineItemCodeGenerator.generateSplitCode(lineItemCode, nextIndex);
            splitItem.setLineItemCode(splitCode);
            splitItem.setParentLineItemCode(lineItemCode);

            newSplitItems.add(splitItem);
            indentInventory.getInventoryList().add(splitItem);
        }

        // Remove the original item from the collection
        indentInventory.getInventoryList().remove(originalItem);

        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }
}