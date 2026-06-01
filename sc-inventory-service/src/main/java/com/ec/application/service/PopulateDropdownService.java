package com.ec.application.service;

import javax.transaction.Transactional;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.StaleAgeBucket;
import com.ec.application.data.StaleBucketConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.application.repository.UsageAreaRepo;
import com.ec.application.repository.WarehouseRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PopulateDropdownService {

    @Autowired
    LocationRepo locRepo;

    @Autowired
    MachineryRepo machineryRepo;

    @Autowired
    WarehouseRepo warehouseRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    CategoryRepo categoryRepo;

    @Autowired
    LocationRepo locationRepo;

    @Autowired
    SupplierRepo supplierRepo;

    @Autowired
    ContractorRepo contractorRepo;

    @Autowired
    UsageAreaRepo usageAreaRepo;

    @Autowired
    BuildingTypeRepo buildingTypeRepo;

    @Value("${boq.enforcement.block:true}")
    private boolean boqEnforcementBlock;

    @Autowired
    SchemaConfig schemaConfig;

    @Autowired
    TenantService tenantService;

    Logger log = LoggerFactory.getLogger(PopulateDropdownService.class);

    public NameAndProjectionDataForDropDown fetchData(String page) {
        NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
        switch (page) {
            // Case machinery on rent
            case "mor":
                morDropdownDataList.setUsagelocation(locationRepo.findIdAndNames());
                morDropdownDataList.setMachinery(machineryRepo.findIdAndNames());
                morDropdownDataList.setSupplier(supplierRepo.findIdAndNames());
                morDropdownDataList.setContractor(contractorRepo.findIdAndNames());
                break;
            // case inward inventory
            case "inward":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setSupplier(supplierRepo.findIdAndNames());
                break;
            case "outward":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setUsagelocation(locationRepo.findIdAndNames());
                morDropdownDataList.setContractor(contractorRepo.findIdAndNames());
                morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
                morDropdownDataList.setUsageArea(usageAreaRepo.findIdAndNames());
                morDropdownDataList.setBuildingtype(buildingTypeRepo.findIdAndNames());
                morDropdownDataList.setUsagelocationWithType(locationRepo.findIdNamesAndTypes());
                morDropdownDataList.setBoqEnforcementBlock(boqEnforcementBlock);
                break;
            case "stock":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
                morDropdownDataList.setProductCodes(productRepo.findIdAndProductCodes());
                break;
            case "lostdamaged":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                break;
            case "allinventory":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
                morDropdownDataList.setContractor(contractorRepo.findIdAndNames());
                morDropdownDataList.setSupplier(supplierRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                break;
            case "inventoryPricing":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                break;
            case "PricingReport":
                morDropdownDataList.setUsagelocation(locationRepo.findIdAndNames());
                break;
            case "indent":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setProductCodes(productRepo.findIdAndProductCodes());
                morDropdownDataList.setIndentStatus(IndentStatusConstants.getAllStatuses());
                morDropdownDataList.setIndentLineItemStatus(IndentLineItemStatusConstants.getAllStatuses());
                morDropdownDataList.setStalebuckets(StaleBucketConstants.getAllBuckets());
                break;
            case "purchaseorder":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setProductCodes(productRepo.findIdAndProductCodes());
                morDropdownDataList.setPurchaseOrderStatus(POStatusConstants.getAllStatuses());
                morDropdownDataList.setSupplier(supplierRepo.findIdAndNames());
                morDropdownDataList.setStalebuckets(StaleBucketConstants.getAllBuckets());
                break;
            case "inventorytransfer":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
                morDropdownDataList.setProductCodes(productRepo.findIdAndProductCodes());
                morDropdownDataList.setTenants(fetchTenantNames());

            case "deadstock":
                morDropdownDataList.setProduct(productRepo.findIdAndNames());
                morDropdownDataList.setProductCodes(productRepo.findIdAndProductCodes());
                morDropdownDataList.setTenants(fetchTenantNames());
        }
        return morDropdownDataList;
    }

    private List<String> fetchTenantNames() {
        List<String> tenantNames = new ArrayList<>(schemaConfig.getSchemaMap().keySet());
        List<String> tenantNamesUpdated = new ArrayList<>();
        for (String tenantName : tenantNames) {
            tenantNamesUpdated.add(tenantName);
        }
        return tenantNamesUpdated;
    }
}
