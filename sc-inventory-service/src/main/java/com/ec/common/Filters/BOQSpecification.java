package com.ec.common.Filters;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.data.BOQReportDto;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BOQStatus_;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.BOQUploadView_;
import com.ec.application.model.BuildingType;
import com.ec.application.model.BuildingType_;
import com.ec.application.model.Category_;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;
import com.ec.application.model.StockInformationFromView;
import com.ec.application.model.StockInformationFromView_;
import com.ec.application.model.UsageLocation;
import com.ec.application.model.UsageLocation_;
import com.ec.application.model.Warehouse_;
import com.ec.application.repository.BOQUploadRepository;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.UsageAreaRepo;

public class BOQSpecification {

	
	@Autowired
	private static ProductRepo productRepository;
	
	@Autowired
	private static BOQUploadRepository bOQUploadRepository;
	
	@Autowired
	private static InwardOutwardListRepo inwardOutwardListRepo;
	

	static SpecificationsBuilder<BOQUpload> specbldr = new SpecificationsBuilder<BOQUpload>();

	public static Specification<BOQUpload> getSpecification(FilterDataList filterDataList) throws Exception
	{
		System.out.println("filterDataList "+filterDataList.getFilterData().toString());
		List<String> buildingType = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "buildingType");
		List<String> usageLocation = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageLocation");
		List<String> products = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "product");
		List<String> categories = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "category");
		List<String> boqStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "status");
		
		System.out.println("percentage "+boqStatus);
		
		Specification<BOQUpload> finalSpec = null;
		 
		 if (products != null && products.size() > 0)
	            finalSpec = specbldr.specAndCondition(finalSpec,
	                    specbldr.whereChildFieldContains(BOQUploadView_.Product,Product_.PRODUCT_NAME ,products));

	     if (categories != null && categories.size() > 0)
	            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldContains(BOQUploadView_.Product,Product_.CATEGORY,Category_.CATEGORY_NAME, categories));

		 if (buildingType != null && buildingType.size() > 0)
         finalSpec = specbldr.specAndCondition(finalSpec,
                 specbldr.whereChildFieldContains(BOQUploadView_.Building_Type,BuildingType_.TYPE_NAME, buildingType));

       if (usageLocation != null && usageLocation.size() > 0)
         finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains(BOQUploadView_.UsageLocation,UsageLocation_.LOCATION_NAME,usageLocation));
      
       if (boqStatus != null && boqStatus.size() > 0)
		{
//			try
//			{
//				for(String boqPercentage:boqStatus)
//				{
//					System.out.println("boqPercentage "+boqPercentage);
//			       String[] split1 = boqPercentage.split("-");
//			          
//			       System.out.println(split1.length);
//					Double split2 = Double.parseDouble(split1[0]);
//					Double split3= Double.parseDouble(split1[1]);
//					System.out.println(split2);
//					System.out.println(split3);
//					
//					List<Object> boqUpload=bOQUploadRepository.findBuildigTypeIdBuildingUnitIdProductId();
//					for (Object object:boqUpload) 
//					{
//						   Object[] objArray= (Object[]) object;
//						   BigInteger bigIntegerNumber0=(BigInteger) objArray[0];
//						   BigInteger bigIntegerNumber1=(BigInteger) objArray[1];
//						   BigInteger bigIntegerNumber2= (BigInteger) objArray[2];
//						   Long buildingType1=bigIntegerNumber0.longValue();
//						   Long buildingUnit=bigIntegerNumber1.longValue();
//						   Long productId= bigIntegerNumber2.longValue();				 
//
//					
//						BOQReportDto bOQReportDto=new BOQReportDto();
//						
//						Product product=productRepository.findByProductId(productId);				
//						List<Object> list=inwardOutwardListRepo.findByOutwardInventory(buildingType1,buildingUnit,productId);
//						if(!list.isEmpty())
//						{
//						
//						long count=0;
//						double outwardQuantity=0.0;
//						for (Object cdata:list) {
//							   Object[] obj= (Object[]) cdata;
//							   if(obj[0]!=null && obj[1]!=null)
//							   {
//								   Double quantity= (Double) obj[5];
//								   outwardQuantity = outwardQuantity+quantity;
//							   count++;
//							   
//							   }
//							    	    
//							  }
//						 Double boqQuantity=bOQUploadRepository.findQuantityByProductProductId(productId);
//						 Double excessQuantity=outwardQuantity-boqQuantity;
//							
//						 int status=(int) (outwardQuantity/boqQuantity*100);
//						
//							finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDoubleBetween(
//							BOQUploadView_.Status, split2,split3));
//
//						 
//							}
//						}
//					}
//				}
//		
//					
////					finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDoubleBetween(
////							BOQUploadView_.Status, split2,split3));
//					
//				
//			 catch (Exception e)
//			{
//				throw new Exception("Unable to parse number value from string");
//			}
		
		}
		return finalSpec;
	}
	
	
}
