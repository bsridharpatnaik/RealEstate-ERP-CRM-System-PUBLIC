package com.ec.application.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.BOQUploadConstant;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.BOQDto;
import com.ec.application.data.BOQReportDto;
import com.ec.application.data.BOQReportResponse;
import com.ec.application.data.BOQStatusDto;
import com.ec.application.data.BOQStatusResponse;
import com.ec.application.data.BOQUploadDto;
import com.ec.application.data.BOQUploadValidationResponse;

import com.ec.application.data.UsageLocationDto;
import com.ec.application.data.UsageLocationResponse;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.BuildingType;
import com.ec.application.model.Category;
import com.ec.application.model.Product;
import com.ec.application.model.UsageArea;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.BOQUploadRepository;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.UsageAreaRepo;

@Service
@Transactional
public class BOQService {

    @Autowired
    private UsageAreaRepo locationRepository;

    @Autowired
    private ProductRepo productRepository;

    @Autowired
    private BuildingTypeRepo buildingTypeRepository;

    @Autowired
    private BOQUploadRepository bOQUploadRepository;

    @Autowired
    private LocationRepo usageLocationRepository;

    @Autowired
    private InwardOutwardListRepo inwardOutwardListRepo;

    Logger log = LoggerFactory.getLogger(BOQService.class);


    public List<BOQUploadValidationResponse> boqUpload(BOQDto boqDto) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse bOQUploadValidationResponse = new BOQUploadValidationResponse();
        List<BOQUploadValidationResponse> listBOQUploadResponse = validateUploadedBOQ(boqDto);
        try {
            if (listBOQUploadResponse.isEmpty()) {
                boqDto.getUpload().forEach(upload -> {
                    Product product = productRepository.findByProductName(upload.getInventory());
                    UsageArea location = locationRepository.findByUsageAreaName(upload.getLocation());
                    BOQUpload boqUpload = bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(), location.getUsageAreaId(), product.getProductId());
                    bOQDetailsModification(upload, product, location, boqUpload);
                });
                bOQUploadValidationResponse.setMessage("Successfully done");
                listBOQUploadResponse.add(bOQUploadValidationResponse);
                return listBOQUploadResponse;
            } else {
                return listBOQUploadResponse;
            }
        } catch (Exception e) {
            bOQUploadValidationResponse.setMessage("Error while uploading the boq details");
            listBOQUploadResponse.add(bOQUploadValidationResponse);
        }
        return new ArrayList<BOQUploadValidationResponse>();
    }


    private void bOQDetailsModification(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)) {
            addBoqRecords(upload, product, location, boqUpload);
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)) {
            updateBoqQuantity(upload, boqUpload);
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
            deleteBoqQuantity(upload, boqUpload);
        }
    }


    private void addBoqRecords(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload == null) {
            save(upload.getBuildingType(), upload.getBuildingUnit(), location.getUsageAreaId(), product.getProductId(), upload.getQuantity(), upload.getSno(), upload.getChanges());
        }
    }


    private void deleteBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload != null) {

            double quantity = 0.0;
            double doublQuantity = Double.parseDouble(upload.getQuantity());
            quantity = boqUpload.getQuantity() - doublQuantity;
            if (quantity == 0) {
                boqUpload.setQuantity(quantity);
                boqUpload.setChanges(upload.getChanges());
                bOQUploadRepository.softDelete(boqUpload);
            } else {
                boqUpload.setQuantity(quantity);
                boqUpload.setChanges(upload.getChanges());
                bOQUploadRepository.save(boqUpload);
            }

        }
    }


    private void updateBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload != null) {
            double quantity = 0.0;
            double doublQquntity = Double.parseDouble(upload.getQuantity());
            quantity = boqUpload.getQuantity() + doublQquntity;
            boqUpload.setQuantity(quantity);
            boqUpload.setChanges(upload.getChanges());
            bOQUploadRepository.save(boqUpload);

        }
    }


    private void save(long buildingTypeId, long buildingUnit, long usageAreaId, long productId, String quantity, int sNo, String changes) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUpload boqUpload = new BOQUpload();

        UsageArea location = locationRepository.findByUsageAreaId(usageAreaId);
        boqUpload.setLocation(location);

        BuildingType buildingType = buildingTypeRepository.findByTypeId(buildingTypeId);
        boqUpload.setBuildingType(buildingType);

        Product product = productRepository.findByProductId(productId);
        boqUpload.setProduct(product);

        double doublQquntity = Double.parseDouble(quantity);
        boqUpload.setQuantity(doublQquntity);
        boqUpload.setChanges(changes);
        boqUpload.setSno(sNo);

        boqUpload.setUsageLocation(usageLocationRepository.findByLocationId((long) buildingUnit));
        bOQUploadRepository.save(boqUpload);
    }


    private List<BOQUploadValidationResponse> validateUploadedBOQ(BOQDto boqDto) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<BOQUploadValidationResponse> listboqBoqUploadResponses = new ArrayList<>();
        List<BOQUpload> listOfBOQUpload = bOQUploadRepository.findAll();
        boqDto.getUpload().forEach(upload -> {
            boolean isInventoryExist = productRepository.existsByProductName(upload.getInventory());
            boolean isLocationExist = locationRepository.existsByUsageAreaName(upload.getLocation());
            try {
                double doublQuantity = Double.parseDouble(upload.getQuantity());
                if (!isInventoryExist || !isLocationExist || !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION) && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE) && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
                    validateInventoryLocationChanges(listboqBoqUploadResponses, upload, isInventoryExist, isLocationExist);
                } else if (doublQuantity < 1) {
                    validateBOQQuantity(listboqBoqUploadResponses, upload);
                } else {
                    validateChanges(listboqBoqUploadResponses, upload, listOfBOQUpload);
                }
            } catch (Exception e) {
                validateBOQQuantity(listboqBoqUploadResponses, upload);
            }
        });

        return listboqBoqUploadResponses;
    }


    private void validateBOQQuantity(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse boqUploadResponse = new BOQUploadValidationResponse();
        List<String> column = new ArrayList<String>();
        column.add(BOQUploadConstant.QUANTITY);
        boqUploadResponse.setSno(upload.getSno());
        boqUploadResponse.setColumns(column);
        listboqBoqUploadResponses.add(boqUploadResponse);
    }


    private void validateChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload, List<BOQUpload> listOfBOQUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Product product = productRepository.findByProductName(upload.getInventory());
        UsageArea location = locationRepository.findByUsageAreaName(upload.getLocation());
        BOQUpload boqUpload = bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(), (long) location.getUsageAreaId(), (long) product.getProductId());
        if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)) {
            if (boqUpload != null) {
                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record will be overwritten");
                listboqBoqUploadResponses.add(boqUploadResponse);
            }
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)) {
            if (boqUpload == null) {
                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record not exist");
                listboqBoqUploadResponses.add(boqUploadResponse);
            }
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
            if (boqUpload == null) {

                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record not exist");
                listboqBoqUploadResponses.add(boqUploadResponse);
            } else {
                BOQUploadValidationResponse boqUploadResponse = setQuantity(upload);
                double quantity = 0.0;
                double doublQuantity = Double.parseDouble(upload.getQuantity());
                quantity = boqUpload.getQuantity() - doublQuantity;
                if (quantity < 0) {
                    boqUploadResponse.setMessage("given quantity exceeds the existing quatity");
                    listboqBoqUploadResponses.add(boqUploadResponse);
                }
            }
        }
    }


    private BOQUploadValidationResponse setInventoryQuantityChangesLocation(BOQUploadDto upload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse boqUploadResponse = new BOQUploadValidationResponse();
        List<String> column = new ArrayList<String>();
        column.add(BOQUploadConstant.INVENTORY);
        column.add(BOQUploadConstant.QUANTITY);
        column.add(BOQUploadConstant.CHANGES);
        column.add(BOQUploadConstant.FINAL_LOCATION);
        boqUploadResponse.setColumns(column);
        boqUploadResponse.setSno(upload.getSno());
        return boqUploadResponse;
    }

    private BOQUploadValidationResponse setQuantity(BOQUploadDto upload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse boqUploadResponse = new BOQUploadValidationResponse();
        List<String> column = new ArrayList<String>();
        column.add(BOQUploadConstant.QUANTITY);
        boqUploadResponse.setColumns(column);
        boqUploadResponse.setSno(upload.getSno());
        return boqUploadResponse;
    }


    private void validateInventoryLocationChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload, boolean existInventory, boolean existLocation) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse bOQUploadResponse = new BOQUploadValidationResponse();
        List<String> columnName = new ArrayList<String>();
        if (!existInventory)
            columnName.add(BOQUploadConstant.INVENTORY);
        if (!existLocation)
            columnName.add(BOQUploadConstant.FINAL_LOCATION);
        if (upload.getChanges().isEmpty()) {
            columnName.add(BOQUploadConstant.CHANGES);
        } else {
            if (!upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION) && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)
                    && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
                columnName.add(BOQUploadConstant.CHANGES);
            }
        }
        bOQUploadResponse.setSno(upload.getSno());
        bOQUploadResponse.setColumns(columnName);

        listboqBoqUploadResponses.add(bOQUploadResponse);
    }


    public BOQStatusResponse getBoqStatusDetails(Long buildingTypeId, Long buildingUnitId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQStatusResponse bOQStatusResponse = new BOQStatusResponse();
        try {
            if (buildingTypeId != null && buildingUnitId != null) {
                List<BOQStatusDto> listBOQStatusResponse = new ArrayList<>();
                List<Integer> boqUpload = bOQUploadRepository.findProductIdByBuildingTypeTypeIdAndUsageLocationLocationIds(buildingTypeId, buildingUnitId);

                getOutwardAndBoqQuantity(buildingTypeId, buildingUnitId, listBOQStatusResponse, boqUpload);

                bOQStatusResponse.setBOQStatusDto(listBOQStatusResponse);
                bOQStatusResponse.setMessage("Get BOQ Status details successfully");
            }
        } catch (Exception e) {
            bOQStatusResponse.setMessage("Error while retrieving the BOQ Status");
        }
        return bOQStatusResponse;
    }


    private void getOutwardAndBoqQuantity(Long buildingTypeId, Long buildingUnitId, List<BOQStatusDto> listBOQStatusResponse, List<Integer> boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<Product> listOfProduct = productRepository.findAll();
        List<Object> listOfOutwardEntry = inwardOutwardListRepo.findByOutwardInventoryByBuildingTypeIdAndBuildingUnitId(buildingTypeId, buildingUnitId);
        List<BOQUpload> listOfBoqQuantity = bOQUploadRepository.findBOQQuantity(buildingTypeId, buildingUnitId);
        boqUpload.forEach(iterateBoqUpload -> {
            BOQStatusDto boqStatusDto = new BOQStatusDto();
            Map<Long, Category> mapCategory = listOfProduct.stream().collect(
                    Collectors.toMap(Product::getProductId, Product::getCategory));
            double outwardQuantity = 0.0;
            long count = 0;
            for (Object objectData : listOfOutwardEntry) {
                Object[] obj = (Object[]) objectData;

                if (obj[0] != null && obj[1] != null) {
                    BigInteger productId = (BigInteger) obj[4];
                    Long longValueOfProduct = productId.longValue();
                    if (longValueOfProduct == iterateBoqUpload.longValue()) {
                        Double quantity = (Double) obj[5];
                        outwardQuantity = outwardQuantity + quantity;
                        count++;
                    }
                }
            }
            getBoqQuantity(listOfBoqQuantity, iterateBoqUpload, boqStatusDto, outwardQuantity);
            Map<Long, String> mapProductName = listOfProduct.stream().collect(
                    Collectors.toMap(Product::getProductId, Product::getProductName));
            boqStatusDto.setInventory(mapProductName.get(iterateBoqUpload.longValue()));
            boqStatusDto.setCategory(mapCategory.get(iterateBoqUpload.longValue()).getCategoryName());
            boqStatusDto.setOutwardQuantity(outwardQuantity);
            listBOQStatusResponse.add(boqStatusDto);
        });
    }


    private void getBoqQuantity(List<BOQUpload> listOfBoqQuantity, Integer iterateBoqUpload,
                                BOQStatusDto boqStatusDto, double outwardQuantity) {
        double boqQuantity = 0.0;
        for (BOQUpload iterateBoqQuantity : listOfBoqQuantity) {
            if (iterateBoqQuantity.getProduct().getProductId() == iterateBoqUpload.longValue()) {
                boqQuantity = boqQuantity + iterateBoqQuantity.getQuantity();
            }
            int status = (int) (outwardQuantity / boqQuantity * 100);
            boqStatusDto.setBoqQuantity(boqQuantity);
            boqStatusDto.setStatus(status);
        }
    }


    public UsageLocationResponse getBuildingUnitByBuildingType(long buildingTypeId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        UsageLocationResponse usageLocationResponse = new UsageLocationResponse();

        List<UsageLocationDto> listusageLocationDtos = new ArrayList<>();
        List<UsageLocation> usageLocation = usageLocationRepository.findByBuildingTypeTypeId(buildingTypeId);
        int count = 0;
        for (UsageLocation usageLocation1 : usageLocation) {
            count++;
            UsageLocationDto usageLocationDtos = new UsageLocationDto();
            usageLocationDtos.setId(usageLocation1.getLoationId());
            usageLocationDtos.setName(usageLocation1.getLocationName());
            listusageLocationDtos.add(usageLocationDtos);
        }
        usageLocationResponse.setUsageLocationCount(count);
        usageLocationResponse.setUsageLocation(listusageLocationDtos);

        return usageLocationResponse;
    }

    public BOQReportResponse getBoqReport() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQReportResponse bOQReportResponse = new BOQReportResponse();
        try {
            List<Product> listOfProduct = productRepository.findAll();
            List<BOQReportDto> listBOQReportResponse = new ArrayList<>();
            List<Object> listOfBuildingTypeBuildingUnitProduct = bOQUploadRepository.findBuildigTypeIdBuildingUnitIdProductId();
            listOfBuildingTypeBuildingUnitProduct.forEach(iterateList -> {

                Object[] objArray = (Object[]) iterateList;
                BigInteger bigIntegerNumber0 = (BigInteger) objArray[0];
                BigInteger bigIntegerNumber1 = (BigInteger) objArray[1];
                BigInteger bigIntegerNumber2 = (BigInteger) objArray[2];
                Long productId = bigIntegerNumber0.longValue();
                Long buildingType = bigIntegerNumber1.longValue();
                Long buildingUnit = bigIntegerNumber2.longValue();

                BOQReportDto bOQReportDto = new BOQReportDto();
                List<Object> list = inwardOutwardListRepo.findByOutwardInventory(buildingType, buildingUnit, productId);
                if (!list.isEmpty()) {
                    getExcessQuantity(listBOQReportResponse, buildingType, buildingUnit, productId, bOQReportDto, listOfProduct, list);
                }
            });
            bOQReportResponse.setMessage("Get BOQ Report successfully");
            bOQReportResponse.setBoqreports(listBOQReportResponse);
        } catch (Exception e) {
            bOQReportResponse.setMessage("Error while retrieving the report");
        }
        return bOQReportResponse;
    }


    private void getExcessQuantity(List<BOQReportDto> listBOQReportResponse, Long buildingType, Long buildingUnit, Long productId, BOQReportDto bOQReportDto, List<Product> listOfProduct, List<Object> list) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        Map<Long, String> mapProductName = listOfProduct.stream().collect(
                Collectors.toMap(Product::getProductId, Product::getProductName));
        Map<Long, String> mapMeasurementUnit = listOfProduct.stream().collect(
                Collectors.toMap(Product::getProductId, Product::getMeasurementUnit));

        long count = 0;
        double outwardQuantity = 0.0;
        for (Object cdata : list) {
            Object[] obj = (Object[]) cdata;
            if (obj[0] != null && obj[1] != null) {
                Double quantity = (Double) obj[5];

                outwardQuantity = outwardQuantity + quantity;
                count++;
            }
        }

        Double boqQuantity = bOQUploadRepository.findQuantityByProductProductId(productId, buildingType, buildingUnit);

        Double excessQuantity = outwardQuantity - boqQuantity;
        if (excessQuantity > 0) {
            Optional<BuildingType> building = buildingTypeRepository.findById(buildingType.longValue());
            Optional<UsageLocation> usageLocation = usageLocationRepository.findById(buildingUnit.longValue());
            bOQReportDto.setBoqQuantity(boqQuantity);
            bOQReportDto.setInventory(mapProductName.get(productId));
            bOQReportDto.setBoqQuantity(boqQuantity);
            bOQReportDto.setBuildingType(building.get().getTypeName());
            bOQReportDto.setBuildingUnit(usageLocation.get().getLocationName());
            bOQReportDto.setOutwardQuantity(outwardQuantity);
            bOQReportDto.setExcessQuantity(excessQuantity);
            bOQReportDto.setMeasurementUnit(mapMeasurementUnit.get(productId));
            listBOQReportResponse.add(bOQReportDto);
        }
    }


}
