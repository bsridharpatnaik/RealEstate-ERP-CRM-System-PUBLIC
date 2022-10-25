package com.ec.application.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.BOQUploadConstant;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.data.BOQDto;
import com.ec.application.data.BOQInformation;
import com.ec.application.data.BOQReportDto;
import com.ec.application.data.BOQReportResponse;
import com.ec.application.data.BOQStatusDataDto;
import com.ec.application.data.BOQStatusDto;
import com.ec.application.data.OutwardQuantityDtoForBoqStatus;
import com.ec.application.data.SingleStockInformationDTO;
import com.ec.application.data.StockInformationDTO;
import com.ec.application.data.BOQStatusResponse;
import com.ec.application.data.BOQStatusView;
import com.ec.application.data.BOQUploadDto;
import com.ec.application.data.BOQUploadValidationResponse;
import com.ec.application.data.BOQStatusDetailsDto;
import com.ec.application.data.BOQStatusDetailsMapKey;
import com.ec.application.data.UsageLocationDto;
import com.ec.application.data.UsageLocationResponse;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.BuildingType;
import com.ec.application.model.Category;
import com.ec.application.model.Product;
import com.ec.application.model.StockInformationFromView;
import com.ec.application.model.UsageArea;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.BOQStatusViewRepository;
import com.ec.application.repository.BOQUploadRepository;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.UsageAreaRepo;
import com.ec.common.Filters.BOQSpecification;
import com.ec.common.Filters.BOQStatusFilterDataList;


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
	
	@Autowired
	private BOQStatusViewRepository bOQStatusViewRepository;
	
	Logger log = LoggerFactory.getLogger(BOQService.class);
	
	Set<Long> buildingTypeIDsForQuery = new HashSet<>();
	Set<Long> buildingUnitIDsForQuery = new HashSet<>();
	
	public List<BOQUploadValidationResponse> boqUpload(BOQDto boqDto)throws Exception {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		BOQUploadValidationResponse bOQUploadValidationResponse=new BOQUploadValidationResponse();
		List<BOQUploadValidationResponse> listBOQUploadResponse = validateUploadedBOQ(boqDto); 
		try
		{	 
			if(listBOQUploadResponse.isEmpty())
			{		
				boqDto.getUpload().forEach(upload->{
					   Product product=productRepository.findByProductName(upload.getInventory());
					   UsageArea location=locationRepository.findByUsageAreaName(upload.getLocation());
					   BOQUpload boqUpload=bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(),location.getUsageAreaId(),product.getProductId());
				bOQDetailsModification(upload, product, location, boqUpload);
			});	
			bOQUploadValidationResponse.setMessage("Successfully done");
			listBOQUploadResponse.add(bOQUploadValidationResponse);
			 return listBOQUploadResponse;
		}
		else
		{
			 return listBOQUploadResponse;
		}   
        }		
		catch (Exception e) {
			bOQUploadValidationResponse.setMessage("Error while uploading the boq details");
			listBOQUploadResponse.add(bOQUploadValidationResponse);
		}
		return new ArrayList<BOQUploadValidationResponse>();
	}


	private void bOQDetailsModification(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION))
		{									   
				     addBoqRecords(upload, product, location, boqUpload);					   								
		}
		else if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE))
		{												   
		             updateBoqQuantity(upload, boqUpload);
		}
		else if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) 
		{											
				     deleteBoqQuantity(upload, boqUpload);					   				
		}
	}


	private void addBoqRecords(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if(boqUpload==null)
		 {					  
			 save(upload.getBuildingType(),upload.getBuildingUnit(),location.getUsageAreaId(),product.getProductId(),upload.getQuantity(),upload.getSno(),upload.getChanges());					    	 
		 }
	}


	private void deleteBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if(boqUpload!=null)
		 {
			
			 boqUpload.setQuantity(0);
			 boqUpload.setChanges(upload.getChanges());
			 bOQUploadRepository.softDelete(boqUpload);

		 }
	}


	private void updateBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if(boqUpload!=null)
		{
			 boqUpload.setChanges(upload.getChanges());
			 boqUpload.setQuantity(Double.parseDouble(upload.getQuantity()));
			 bOQUploadRepository.save(boqUpload);
			 
		}
	}
	

	private void save(long buildingTypeId, long buildingUnit, long usageAreaId, long productId, String quantity, int sNo, String changes) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		        BOQUpload boqUpload=new BOQUpload();
		        
				UsageArea location=locationRepository.findByUsageAreaId(usageAreaId);				
				boqUpload.setLocation(location);
				
				BuildingType buildingType=buildingTypeRepository.findByTypeId(buildingTypeId);
				boqUpload.setBuildingType(buildingType);
				
				Product product=productRepository.findByProductId(productId);
				boqUpload.setProduct(product);
				
				double doublQquntity=Double.parseDouble(quantity);
				boqUpload.setQuantity(doublQquntity);
				boqUpload.setChanges(changes);
				boqUpload.setSno(sNo);
		
			boqUpload.setUsageLocation(usageLocationRepository.findByLocationId((long)buildingUnit));
			bOQUploadRepository.save(boqUpload);
	}


   private List<BOQUploadValidationResponse> validateUploadedBOQ(BOQDto boqDto) throws Exception {
	   log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<BOQUploadValidationResponse> listboqBoqUploadResponses=new ArrayList<>();
	    List<BOQUpload> listOfBOQUpload=bOQUploadRepository.findAll();
			boqDto.getUpload().forEach(upload->{
				boolean isInventoryExist=productRepository.existsByProductName(upload.getInventory());    
				boolean isLocationExist=locationRepository.existsByUsageAreaName(upload.getLocation());      			
				try
				{
				double doublQuantity=Double.parseDouble(upload.getQuantity());		
				if(!isInventoryExist || !isLocationExist || !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION) && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE) && !upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
					validateInventoryLocationChanges(listboqBoqUploadResponses, upload, isInventoryExist, isLocationExist);
				}	
				else
				{ 
					validateChanges(listboqBoqUploadResponses, upload,listOfBOQUpload);
				}
				}
				catch (Exception e) {
					validateBOQQuantity(listboqBoqUploadResponses, upload);
				}
			});	

		return listboqBoqUploadResponses;
	}


		private void validateBOQQuantity(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			BOQUploadValidationResponse boqUploadResponse =new BOQUploadValidationResponse();
			List<String> column=new ArrayList<String>();
			column.add(BOQUploadConstant.QUANTITY);
			boqUploadResponse.setSno(upload.getSno());
			boqUploadResponse.setColumns(column);	              
			listboqBoqUploadResponses.add(boqUploadResponse);
		}


		private void validateChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload,List<BOQUpload> listOfBOQUpload) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			  Product product=productRepository.findByProductName(upload.getInventory());
			  UsageArea location=locationRepository.findByUsageAreaName(upload.getLocation());
			  BOQUpload boqUpload=bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(),(long)location.getUsageAreaId(),(long)product.getProductId());
			if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)){
			  if(boqUpload!=null){
				  BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
					boqUploadResponse.setMessage("record will be overwritten");
					listboqBoqUploadResponses.add(boqUploadResponse);
			  }				
			}
			else if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)){
			  if(boqUpload==null){
				  BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
					boqUploadResponse.setMessage("record not exist");
					listboqBoqUploadResponses.add(boqUploadResponse);
			  }				
			}
			else if(upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)){					
			  if(boqUpload==null){
				  
				  BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
					boqUploadResponse.setMessage("record not exist");
					listboqBoqUploadResponses.add(boqUploadResponse);
			  }
			  }
			}


		private BOQUploadValidationResponse setInventoryQuantityChangesLocation(BOQUploadDto upload) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			BOQUploadValidationResponse boqUploadResponse =new BOQUploadValidationResponse();
				List<String> column=new ArrayList<String>();
				column.add(BOQUploadConstant.INVENTORY);
				column.add(BOQUploadConstant.QUANTITY);
				column.add(BOQUploadConstant.CHANGES);
				column.add(BOQUploadConstant.FINAL_LOCATION);
				boqUploadResponse.setColumns(column);
				boqUploadResponse.setSno(upload.getSno());
			return boqUploadResponse;
		}
		
		


		private void validateInventoryLocationChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses,BOQUploadDto upload, boolean existInventory, boolean existLocation) {
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



			
		public UsageLocationResponse getBuildingUnitByBuildingType(long buildingTypeId) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		    UsageLocationResponse usageLocationResponse=new UsageLocationResponse();

			List<UsageLocationDto> listusageLocationDtos=new ArrayList<>();
			List<UsageLocation> usageLocation=usageLocationRepository.findByBuildingTypeTypeId(buildingTypeId);
			int count=0;
			for(UsageLocation usageLocation1:usageLocation)
			{
				count++;
				UsageLocationDto usageLocationDtos=new UsageLocationDto();
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
			BOQReportResponse bOQReportResponse=new BOQReportResponse();
		try
		{
			List<Product> listOfProduct=productRepository.findAll();
			List<BOQReportDto> listBOQReportResponse=new ArrayList<>();
			List<Object> listOfBuildingTypeBuildingUnitProduct=bOQUploadRepository.findBuildigTypeIdBuildingUnitIdProductId();
			listOfBuildingTypeBuildingUnitProduct.forEach(iterateList->{
				
				   Object[] objArray= (Object[]) iterateList;
				   BigInteger bigIntegerNumber0=(BigInteger) objArray[0];
				   BigInteger bigIntegerNumber1=(BigInteger) objArray[1];
				   BigInteger bigIntegerNumber2= (BigInteger) objArray[2];
				   Long productId= bigIntegerNumber0.longValue();
				   Long buildingType=bigIntegerNumber1.longValue();
				   Long buildingUnit=bigIntegerNumber2.longValue();
				  	   				   
						BOQReportDto bOQReportDto=new BOQReportDto();
						List<Object> list=inwardOutwardListRepo.findByOutwardInventory(buildingType,buildingUnit,productId);
						if(!list.isEmpty()){
						  getExcessQuantity(listBOQReportResponse, buildingType, buildingUnit, productId, bOQReportDto,listOfProduct,list);
						}
			});
			bOQReportResponse.setMessage("Get BOQ Report successfully");
			bOQReportResponse.setBoqreports(listBOQReportResponse);
			}
		catch (Exception e) {
			bOQReportResponse.setMessage("Error while retrieving the report");
		}
		return bOQReportResponse;
	}


		private void getExcessQuantity(List<BOQReportDto> listBOQReportResponse, Long buildingType, Long buildingUnit,Long productId, BOQReportDto bOQReportDto, List<Product> listOfProduct, List<Object> list) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			Map<Long, String> mapProductName = listOfProduct.stream().collect(
		    Collectors.toMap(Product::getProductId, Product::getProductName));
			Map<Long, String> mapMeasurementUnit = listOfProduct.stream().collect(
		    Collectors.toMap(Product::getProductId, Product::getMeasurementUnit));
			long count=0;
			double outwardQuantity=0.0;
			for (Object cdata:list) {
				   Object[] obj= (Object[]) cdata;
				   if(obj[1]!=null && obj[2]!=null){
					   Double quantity= (Double) obj[5];
					    outwardQuantity = outwardQuantity+quantity;
				   count++;
				   }}
			 Double boqQuantity=bOQUploadRepository.findQuantityByProductProductId(productId,buildingType,buildingUnit); 
			 Double excessQuantity=outwardQuantity-boqQuantity;
			 if(excessQuantity>0){
				Optional<BuildingType> building=buildingTypeRepository.findById(buildingType.longValue());	
				Optional<UsageLocation> usageLocation=usageLocationRepository.findById(buildingUnit.longValue());
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


		public BOQInformation fetchBoqStatusInformation(BOQStatusFilterDataList bOQStatusFilterDataList, Pageable page) throws Exception {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
	           	BOQInformation bOQInformation = new BOQInformation();	            
	           	Specification<BOQStatusView> specification = BOQSpecification.getSpecification(bOQStatusFilterDataList);	
	           	Page<BOQStatusView> bOQStatusView = bOQStatusViewRepository.findAll(specification, page);	           	           	
	           	for(BOQStatusView view : bOQStatusView) {
	           	getBuildingTypeIdAndBuildingUnitId(view);
	           	}	         		           	 	
	           	List<BOQStatusDto> listOfBOQStatusDto = convertBOQStatusToDTO(bOQStatusView.getContent());		
            	if(page.getSort().isUnsorted())
	           	{
	           		Page<BOQStatusDto> boqStatusPage = new PageImpl<BOQStatusDto>(listOfBOQStatusDto, page,bOQStatusView.getTotalElements());
					bOQInformation.setBoqstatusDto(boqStatusPage);  
	           	}
	           	else
	           	{
	           		SortingOfOutwardQuantityAndStatus(page, bOQInformation, specification, bOQStatusView,listOfBOQStatusDto);
	           	}	           	
			         return bOQInformation;     
	}


		private void SortingOfOutwardQuantityAndStatus(Pageable page, BOQInformation bOQInformation,
				Specification<BOQStatusView> specification, Page<BOQStatusView> bOQStatusView,
				List<BOQStatusDto> listOfBOQStatusDto) {
			String[] sortBy = page.getSort().toString().split(":");
			String field = sortBy[0].trim();
			String order = sortBy[1].trim();
			if(field.equals("outwardQuantity") || field.equals("status"))
			{	
			   	List<BOQStatusView> listOfBoqStatusView=bOQStatusViewRepository.findAll(specification);
				List<BOQStatusDto> convertListOfBOQStatusDto = convertBOQStatusToDTO(listOfBoqStatusView);
			    int start = (int) page.getOffset();
			    int end = (start + page.getPageSize()) > convertListOfBOQStatusDto.size() ? convertListOfBOQStatusDto.size(): (start + page.getPageSize());
				List<BOQStatusDto> list=sortBoqStatusList(convertListOfBOQStatusDto,field,order);
				Page<BOQStatusDto> boqStatusPage = new PageImpl<BOQStatusDto>(list.subList(start, end), page,bOQStatusView.getTotalElements());
				bOQInformation.setBoqstatusDto(boqStatusPage);
			}else {
				Page<BOQStatusDto> boqStatusPage = new PageImpl<BOQStatusDto>(listOfBOQStatusDto, page,bOQStatusView.getTotalElements());
				bOQInformation.setBoqstatusDto(boqStatusPage);
			}
		}


		private List<BOQStatusDto> sortBoqStatusList(List<BOQStatusDto> listOfBOQStatusDto, String field, String order) {
	        try {	          
	            switch (field) {	
	                case "outwardQuantity":
	                    if (order.toLowerCase().contains("desc"))
	                    	listOfBOQStatusDto.sort(Comparator.comparing(BOQStatusDto::getOutwardQuantity,
	                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
	                    else
	                    	listOfBOQStatusDto.sort(Comparator.comparing(BOQStatusDto::getOutwardQuantity,
	                                Comparator.nullsFirst(Comparator.naturalOrder())));
	                    break;
	                
	                case "status":
	                    if (order.toLowerCase().contains("desc"))
	                    	listOfBOQStatusDto.sort(Comparator
	                                .comparing(BOQStatusDto::getStatus, Comparator.nullsFirst(Comparator.naturalOrder()))
	                                .reversed());
	                    else
	                    	listOfBOQStatusDto.sort(Comparator.comparing(BOQStatusDto::getStatus,
	                                Comparator.nullsFirst(Comparator.naturalOrder())));
	                    break;                      
	            }
	            return listOfBOQStatusDto;
	        } catch (Exception e) {
	            return listOfBOQStatusDto;
	        }
		}


		private List<BOQStatusDto> convertBOQStatusToDTO(List<BOQStatusView> bOQStatusView) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			List<Object> listOfOutwardQuantity=bOQUploadRepository.findOutwardQuantityByBuildingTypeIdAndUsageLocationId(buildingTypeIDsForQuery,buildingUnitIDsForQuery);
			List<Object> listOfboqDetails=bOQUploadRepository.findUsageAreaBoqQuantityOutwardQuantityByBuildingTypeIdAndUsageLocationId(buildingTypeIDsForQuery,buildingUnitIDsForQuery);
			
			Map<BOQStatusDetailsMapKey, Double> mapOutwardQuantity = getOutwardQuantty(listOfOutwardQuantity);	
			Map<BOQStatusDetailsMapKey, List<BOQStatusDetailsDto>> mapOfBOQStatusDetails = getBoqStatusDetails(listOfboqDetails);			
			List<BOQStatusDto> listOfBOQStatusDto=new ArrayList<>();
			for(BOQStatusView iteratebOQStatusView:bOQStatusView){
			    BOQStatusDto bOQStatusDto=new BOQStatusDto();			     
			    bOQStatusDto.setId(iteratebOQStatusView.getId()); 			    	
			    bOQStatusDto.setCategory(iteratebOQStatusView.getCategory());
			    bOQStatusDto.setProduct(iteratebOQStatusView.getProduct());
			    bOQStatusDto.setBoqQuantity(iteratebOQStatusView.getBoqQuantity());		           
			    bOQStatusDto.setBuildingUnit(iteratebOQStatusView.getBuildingUnit());	    
				BOQStatusDetailsMapKey key = new BOQStatusDetailsMapKey();
				key.setUsageLocationId(String.valueOf(iteratebOQStatusView.getUsageLocationId()));
				key.setProductId(String.valueOf(iteratebOQStatusView.getProductId()));						    	
			    if(mapOutwardQuantity.get(key)!=null){
			    	 bOQStatusDto.setOutwardQuantity(mapOutwardQuantity.get(key));
			    	 bOQStatusDto.setStatus((int)(mapOutwardQuantity.get(key)/iteratebOQStatusView.getBoqQuantity()*100));
			    }		
			    List<BOQStatusDetailsDto> listOfBoqStatusDetailsDto=mapOfBOQStatusDetails.get(key);
			    bOQStatusDto.setBoqDetails(listOfBoqStatusDetailsDto);
			    listOfBOQStatusDto.add(bOQStatusDto);
			}
			return listOfBOQStatusDto;
		}
		
		private Map<BOQStatusDetailsMapKey, Double> getOutwardQuantty(List<Object> listOfOutwardQuantity) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			List<OutwardQuantityDtoForBoqStatus> listOfOutwardQuantityForBoqStatusDto = new ArrayList<>();
			Map<BOQStatusDetailsMapKey,Double> mapOutwardQuantity= new HashMap<>();
			for (Object objectData:listOfOutwardQuantity) {
				Object[] obj= (Object[]) objectData;					     	 
				OutwardQuantityDtoForBoqStatus outwardQuantityDtoForBoqStatus=new OutwardQuantityDtoForBoqStatus();
				Double quantity= (Double) obj[0];
			    BigInteger bigIntegerUsageLocationId=(BigInteger)obj[2];				    	 
				Long usageLocationId=bigIntegerUsageLocationId.longValue();			    	 
				BigInteger bigIntegerProductId=(BigInteger)obj[3];				    	 
				Long productId=bigIntegerProductId.longValue();						        					        
			    BOQStatusDetailsMapKey key = new BOQStatusDetailsMapKey();
				key.setUsageLocationId(String.valueOf(usageLocationId));
				key.setProductId(String.valueOf(productId));
			    outwardQuantityDtoForBoqStatus.setOutwardQuantity(quantity);
				outwardQuantityDtoForBoqStatus.setbOQStatusDetailsMapKey(key);
				listOfOutwardQuantityForBoqStatusDto.add(outwardQuantityDtoForBoqStatus);						        
				 mapOutwardQuantity.put(key, quantity);		   
			}		    
			return mapOutwardQuantity;
		}


	
		private Map<BOQStatusDetailsMapKey, List<BOQStatusDetailsDto>> getBoqStatusDetails(List<Object> listOfboqDetails) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
			Map<BOQStatusDetailsMapKey,List<BOQStatusDetailsDto>> mapOfBOQStatusDetails= new HashMap<>();
			List<BOQStatusDetailsDto> listOfBOQStatusDetailsDto=new ArrayList<>(); 
			for (Object objectData:listOfboqDetails) {				
			   Object[] obj= (Object[]) objectData;			   
			   BOQStatusDetailsDto bOQStatusDetailsDto=new BOQStatusDetailsDto();	   
			        Integer id=(Integer)obj[0];
				    BigInteger bigIntegerProductId=(BigInteger)obj[7];				    	 
			    	Long productId=bigIntegerProductId.longValue();			    	
			        Double outwardQuantity= (Double) obj[2];
			        Double boqQuantity= (Double) obj[3];			       
			        BigInteger bigIntegerUsageLocationId=(BigInteger)obj[4];				    	 
				    Long usageLocationId=bigIntegerUsageLocationId.longValue();				    
				    String finalLocationName=(String)obj[5];					
				    BOQStatusDetailsMapKey bOQStatusDetailsMapKey=new BOQStatusDetailsMapKey();
				    bOQStatusDetailsMapKey.setUsageLocationId(String.valueOf(usageLocationId));
				    bOQStatusDetailsMapKey.setProductId(String.valueOf(productId));
				    bOQStatusDetailsDto.setBOQStatusDetailsMapKey(bOQStatusDetailsMapKey);			    
			        bOQStatusDetailsDto.setFinalLocation(finalLocationName);
			        bOQStatusDetailsDto.setBoqQuantity(boqQuantity);
			        bOQStatusDetailsDto.setOutwardQuantity(outwardQuantity);
			        listOfBOQStatusDetailsDto.add(bOQStatusDetailsDto);				        			        
			        List<BOQStatusDetailsDto> filterlistOfBOQStatusDetailsDto=new ArrayList<>();
			        for(BOQStatusDetailsDto iterateOfBOQStatusDetailsDto: listOfBOQStatusDetailsDto){		        	 	
			        	if(iterateOfBOQStatusDetailsDto.getBOQStatusDetailsMapKey().getUsageLocationId().equals(String.valueOf(usageLocationId)) && iterateOfBOQStatusDetailsDto.getBOQStatusDetailsMapKey().getProductId().equals(String.valueOf(productId))){
			        		filterlistOfBOQStatusDetailsDto.add(iterateOfBOQStatusDetailsDto);
			        	}
			        }
			         mapOfBOQStatusDetails.put(bOQStatusDetailsMapKey, filterlistOfBOQStatusDetailsDto);		   
			}
			return mapOfBOQStatusDetails;
		}
		

		
	 private void getBuildingTypeIdAndBuildingUnitId(BOQStatusView bOQStatusView) {
		 log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		 	 			      
			 buildingTypeIDsForQuery.add(bOQStatusView.getBuildingTypeId());
			 buildingUnitIDsForQuery.add(bOQStatusView.getUsageLocationId());		 
		 		
	 }


	

}
