package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.InwardInventorySpecification;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.config.IndentStatusConstants;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.*;
import com.ec.application.model.APICallTypeForAuthorization;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.InwardInventory;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        validateInputs(iiData);
        setFields(indentInventory, iiData);
        indentInventoryRepo.save(indentInventory);
        return indentInventory;
    }

    private void setFields(IndentInventory indentInventory, IndentInventoryData iiData) {
        indentInventory.setTenantSchemaCode(schemaConfig.getSchemaCode(ThreadLocalStorage.getTenantName()));
        indentInventory.setIndentDate(iiData.getIndentDate());
        indentInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        indentInventory.setIndentStatus(IndentStatusConstants.STATUS_CREATED);
        indentInventory.setInventoryList(fetchIndentProductList(iiData.getInventoryList()));
    }

    private List<IndentInventoryList> fetchIndentProductList(List<IndentProductDTO> indentProductDTOs) {
        List<IndentInventoryList> reuturnList = new ArrayList<>();
        for(IndentProductDTO indentProductDTO : indentProductDTOs) {
            IndentInventoryList indentInventoryList = new IndentInventoryList();
            indentInventoryList.setProduct(productRepo.findByProductId(indentProductDTO.getProductId()));
            indentInventoryList.setQuantity(indentProductDTO.getQuantity());
            indentInventoryList.setRemarks(indentProductDTO.getRemarks());
            indentInventoryList.setSpecification(indentProductDTO.getSpecification());
            indentInventoryList.setMeasurementUnit(indentInventoryList.getProduct().getMeasurementUnit());
            reuturnList.add(indentInventoryList);
        }
        return reuturnList;
    }

    private void validateInputs(IndentInventoryData iiData) throws Exception {

        if (iiData.getIndentDate() == null)
            throw new Exception("Purchase Order Date is a mandatory field");

        for(IndentProductDTO indentProductDTO : iiData.getInventoryList()) {
            if (!productRepo.existsById(indentProductDTO.getProductId()))
                throw new Exception("Product not found with ID " + indentProductDTO.getProductId());
            if (indentProductDTO.getQuantity() <= 0)
                throw new Exception("Quantity cannot be less that or equals zero");
        }

        Long duplicateProductIdCount = iiData.getInventoryList().stream()
                .collect(Collectors.groupingBy(IndentProductDTO::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");

    }

    public ReturnIndentInventoryData fetchIndentInventory(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        ReturnIndentInventoryData returnData = new ReturnIndentInventoryData();
        Specification<IndentInventory> spec = IndentInventorySpecification.getSpecification(filterDataList);

        // Feed listing
        if (spec != null)
            returnData.setIndentInventories(indentInventoryRepo.findAll(spec, pageable));
        else
            returnData.setIndentInventories(indentInventoryRepo.findAll(pageable));

        // Feed dropdowns
        //returnInwardInventoryData.setIiDropdown(populateDropdownService.fetchData("inward"));
        return returnData;
    }

}
