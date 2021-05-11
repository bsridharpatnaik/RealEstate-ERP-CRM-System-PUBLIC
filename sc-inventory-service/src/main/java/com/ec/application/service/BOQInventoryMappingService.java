package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.BOQCreateRequestData;
import com.ec.application.data.BOQUpdateRequestData;
import com.ec.application.model.BOQInventoryMapping;
import com.ec.application.model.BOQLocationTypeEnum;
import com.ec.application.model.BuildingType;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.BOQInventoryMappingRepo;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.ProductRepo;

@Service
@Transactional
public class BOQInventoryMappingService {

    @Autowired
    BOQInventoryMappingRepo bimRepo;

    @Autowired
    BuildingTypeRepo btRepo;

    @Autowired
    LocationRepo lRepo;

    @Autowired
    ProductRepo pRepo;

    Logger log = LoggerFactory.getLogger(BOQInventoryMappingService.class);

    public void createNewBOQ(BOQCreateRequestData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        exitIfCombinationExists(payload);
        BOQInventoryMapping bim = new BOQInventoryMapping();
        setFields(bim, payload);
        bimRepo.save(bim);
    }

    public BOQInventoryMapping updateBOQ(BOQUpdateRequestData payload, Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validateUpdatePayload(payload, id);
        BOQInventoryMapping bim = bimRepo.findById(id).get();
        bim.setQuantity(payload.getQuantity());
        bimRepo.save(bim);
        return bim;
    }

    public void deleteBOQEntry(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!bimRepo.existsById(id))
            throw new Exception("BOQ Record not found with ID - " + id);
        bimRepo.softDeleteById(id);
    }

    public Page<BOQInventoryMapping> getBOQByType(Long id, Pageable pageable) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!btRepo.existsById(id))
            throw new Exception("Building Type record not found with ID - " + id);

        Page<BOQInventoryMapping> bimList = bimRepo.getBIMbyType(id, pageable);
        return bimList;
    }

    public Page<BOQInventoryMapping> getBOQByLocation(Long id, Pageable pageable) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!lRepo.existsById(id))
            throw new Exception("Building Unit record not found with ID - " + id);

        Page<BOQInventoryMapping> bimList = bimRepo.getBIMbyLocation(id, pageable);
        return bimList;
    }

    public BOQInventoryMapping getOne(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (id == null)
            throw new Exception("BOQ ID cannot be null or empty");

        Optional<BOQInventoryMapping> bimOptional = bimRepo.findById(id);

        if (!bimOptional.isPresent())
            throw new Exception("BOQ Record not found by ID - " + id);
        return bimOptional.get();
    }

    private void validateUpdatePayload(BOQUpdateRequestData payload, Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (id == null)
            throw new Exception("BOQ ID cannot be null or empty for updating");

        if (!bimRepo.existsById(id))
            throw new Exception("BOQ Record not found with ID - " + id);

        if (bimRepo.findById(id).get().getProduct().getProductId().equals(payload.getProductId()) == false) {
            throw new Exception(
                    "Inventory cannot be modified while updating BOQ record. Please delete and add new if required");
        }

        if (payload.getQuantity() == 0)
            throw new Exception("Quantity cannot be zero or empty. Please input valid quantity");
    }

    private void setFields(BOQInventoryMapping bim, BOQCreateRequestData payload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType)) {
            bim.setBuildingType(btRepo.findById(payload.getId()).get());
            bim.setBoqType(BOQLocationTypeEnum.BuildingType);

        } else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit)) {
            bim.setLocation(lRepo.findById(payload.getId()).get());
            bim.setBoqType(BOQLocationTypeEnum.BuildingUnit);
        }
        bim.setProduct(pRepo.findById(payload.getProductId()).get());
        bim.setQuantity(payload.getQuantity());
    }

    private void exitIfCombinationExists(BOQCreateRequestData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType)) {
            BuildingType bt = btRepo.findById(payload.getId()).get();
            List<BOQInventoryMapping> bimList = bimRepo.findByBuildingTypeAndProduct(bt, payload.getProductId());
            if (bimList.size() > 0)
                throw new Exception("Inventory already exists for Building Type -" + bt.getTypeName());
        } else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit)) {
            UsageLocation ul = lRepo.findById(payload.getId()).get();
            List<BOQInventoryMapping> bimList = bimRepo.findByLocationProduct(ul, payload.getProductId());
            if (bimList.size() > 0)
                throw new Exception("Inventory already exists for Building Unit -" + ul.getLocationName());
        }
    }

    private void validatePayload(BOQCreateRequestData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getBoqType() == null)
            throw new Exception("BOQ Type cannot be null or empty");

        if (payload.getId() == null)
            throw new Exception("Required field ID cannot be null or empty");
        if (payload.getProductId() == null)
            throw new Exception("Required field ProductID cannot be null or empty");
        if (payload.getQuantity() == null || payload.getQuantity() <= 0)
            throw new Exception("Required field Quantity cannot be empty OR less than 1");

        if (!pRepo.existsById(payload.getProductId()))
            throw new Exception("Product not found with ID -" + payload.getProductId());

        if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType)) {
            if (!btRepo.existsById(payload.getId()))
                throw new Exception("Building Type not found with ID -" + payload.getId());
        } else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit)) {
            if (!lRepo.existsById(payload.getId()))
                throw new Exception("Building Unit not found with ID -" + payload.getId());
        } else {
            throw new Exception("Unknown type for field boqtype");
        }
        if (payload.getQuantity() == 0)
            throw new Exception("Quantity cannot be zero or empty. Please input valid quantity");
    }

    public List<IdNameProjections> getProductListForDropdown(BOQLocationTypeEnum boqType, Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<IdNameProjections> masterProductList = pRepo.findIdAndNames();
        List<IdNameProjections> usedProductList = new ArrayList<IdNameProjections>();
        List<IdNameProjections> finalList = new ArrayList<IdNameProjections>();
        if (boqType.equals(BOQLocationTypeEnum.BuildingType)) {
            if (!btRepo.existsById(id))
                throw new Exception("Building Type not found with ID - " + id);
            usedProductList = bimRepo.findUsedProductListForType(id);
        } else if (boqType.equals(BOQLocationTypeEnum.BuildingUnit)) {
            if (!lRepo.existsById(id))
                throw new Exception("Building Unit not found with ID - " + id);
            usedProductList = bimRepo.findUsedProductListForUnit(id);
        }
        if (usedProductList == null)
            throw new Exception("Not able to fetch used product list");

        for (IdNameProjections i : masterProductList) {
            boolean isPresent = false;
            for (IdNameProjections j : usedProductList) {
                if (i.getId().equals(j.getId())) {
                    isPresent = true;
                    break;
                }
            }
            if (!isPresent)
                finalList.add(i);
        }
        return finalList;
    }
}
