package com.ec.application.service;

import com.ec.application.data.IMPPCreateDTO;
import com.ec.application.data.IMPPDeleteDTO;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.InventoryMonthPriceMappingRepository;
import com.ec.application.repository.ProductRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.InventoryMonthPriceMappingSpecification;
import com.ec.common.Filters.MissingInventoryPricingSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@Transactional
public class InventoryMonthPriceMappingService {

    @Autowired
    private InventoryMonthPriceMappingRepository inventoryMonthPriceMappingRepository;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;
    Logger log = LoggerFactory.getLogger(InventoryMonthPriceMappingService.class);

    public InventoryMonthPriceMapping createInventoryMonthPriceMapping(IMPPCreateDTO ipData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        InventoryMonthPriceMapping inventoryMonthPriceMapping;
        validateInput(ipData);
        List<InventoryMonthPriceMapping> impList = inventoryMonthPriceMappingRepository.findByInventoryMonth(ipData.getProductId(), stringToDate(ipData.getDate()));
        if (impList.size() > 0) {
            inventoryMonthPriceMapping = impList.get(0);
            inventoryMonthPriceMapping.setPrice(ipData.getPrice());
        } else {
            inventoryMonthPriceMapping = new InventoryMonthPriceMapping();
            populateValues(ipData, inventoryMonthPriceMapping);
        }
        return inventoryMonthPriceMappingRepository.save(inventoryMonthPriceMapping);
    }

    private Date stringToDate(String date) throws ParseException {
        Date newDate = new SimpleDateFormat("yyyy/MM").parse(date);
        return newDate;
    }

    public void deleteInventoryMonthPriceMapping(IMPPDeleteDTO ipData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<InventoryMonthPriceMapping> impList = inventoryMonthPriceMappingRepository.findByInventoryMonth(ipData.getProductId(), ipData.getDate());
        if (impList.size() > 0) {
            impList.forEach(i -> inventoryMonthPriceMappingRepository.softDeleteById(i.getId()));
        } else
            throw new Exception("Record with combined inventory month price mapping was not found");
    }

    private void populateValues(IMPPCreateDTO ipData, InventoryMonthPriceMapping inventoryMonthPriceMapping) throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inventoryMonthPriceMapping.setPrice(ipData.getPrice());
        inventoryMonthPriceMapping.setDate(stringToDate(ipData.getDate()));
        inventoryMonthPriceMapping.setProduct(productRepo.findByProductId(ipData.getProductId()));
    }

    private void validateInput(IMPPCreateDTO ipData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (ipData.getDate() == null || ipData.getPrice() == null || ipData.getProductId() == null)
            throw new Exception("One/More of required fields are missing!");
        if (!productRepo.existsById(ipData.getProductId()))
            throw new Exception("Product not found with id !");
        if (ipData.getPrice() < 0)
            throw new Exception("Price cannot be negative!");
    }

    public Page<InventoryMonthPriceMapping> findAllInventoryMonthPriceMapping(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<InventoryMonthPriceMapping> spec = InventoryMonthPriceMappingSpecification.getSpecification(filterDataList);
        if (spec != null)
            return inventoryMonthPriceMappingRepository.findAll(spec, pageable);
        return inventoryMonthPriceMappingRepository.findAll(pageable);
    }

    public NameAndProjectionDataForDropDown getDropDownValues() {
        return populateDropdownService.fetchData("inventoryPricing");
    }
}
