package com.ec.application.service;

import com.ec.application.data.InventoryLocationUsageDTO;
import com.ec.application.data.InventoryUsagePayload;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.repository.InventoryMonthUsageInformationRepo;
import com.ec.application.repository.MissingInventoryPricingRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryMonthUsageInformationService {

    @Autowired
    InventoryMonthUsageInformationRepo inventoryMonthUsageInformationRep;

    @Autowired
    MissingInventoryPricingRepo missingInventoryPricingRepo;

    @Autowired
    PopulateDropdownService populateDropdown;

    Logger log = LoggerFactory.getLogger(InventoryMonthUsageInformationService.class);

    public List<InventoryLocationUsageDTO> fetchMonthlyUsageInformation(InventoryUsagePayload payload, Pageable pageable) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validateAndPopulatePayload(payload);

        List<InventoryLocationUsageDTO> resultSet = inventoryMonthUsageInformationRep.findInventoryUsagePricing(payload.getLocationId(), payload.getMonthList());
        for (InventoryLocationUsageDTO d : resultSet) {
            if (d.getTotalPrice() == null)
                throw new Exception("Pricing information is missing for one or more products. Please add required pricing information.");
        }
        return resultSet;
    }

    private void validateAndPopulatePayload(InventoryUsagePayload payload) throws Exception {
        if (payload.getLocationId() == null)
            throw new Exception("Required field Location details not provided!");

        if (payload.getMonthList().size() == 0)
            inventoryMonthUsageInformationRep.findDatesByLocation(payload.getLocationId()).forEach(i -> payload.getMonthList().add(i));
    }

    public List<String> findDatesByLocation(Long locationId) {
        return inventoryMonthUsageInformationRep.findDatesByLocation(locationId);
    }

    public NameAndProjectionDataForDropDown getDropdown() {
        return populateDropdown.fetchData("PricingReport");
    }
}
