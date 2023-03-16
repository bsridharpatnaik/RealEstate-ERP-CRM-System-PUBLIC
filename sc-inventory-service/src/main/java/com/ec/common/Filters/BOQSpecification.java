package com.ec.common.Filters;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQStatusView;
import com.ec.application.model.BOQUploadView_;
import com.ec.application.repository.BOQUploadRepository;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.ProductRepo;

public class BOQSpecification {

	
	@Autowired
	private static ProductRepo productRepository;
	
	@Autowired
	private static BOQUploadRepository bOQUploadRepository;
	
	@Autowired
	private static InwardOutwardListRepo inwardOutwardListRepo;
	

	static SpecificationsBuilder<BOQStatusView> specbldr = new SpecificationsBuilder<BOQStatusView>();

	public static Specification<BOQStatusView> getSpecification(BOQStatusFilterDataList filterDataList) throws Exception
	{
		System.out.println("filterDataList "+filterDataList.getFilterData().toString());
		
		List<String> buildingType = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "buildingType");
		List<String> buildingUnits = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "buildingUnit");
		
		List<String> products = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "product");
		List<String> categories = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "category");
		List<String> boqStatus = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "status");
		
		System.out.println("percentage "+boqStatus);
		
		Specification<BOQStatusView> finalSpec = null;
		 
		 if (products != null && products.size() > 0)
	            finalSpec = specbldr.specAndCondition(finalSpec,
	                    specbldr.whereDirectFieldContains(BOQUploadView_.Product ,products));

	     if (categories != null && categories.size() > 0)
	            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQUploadView_.Category, categories));

		 if (buildingType != null && buildingType.size() > 0)
         finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(BOQUploadView_.Building_Type, buildingType));

       if (buildingUnits != null && buildingUnits.size() > 0)
         finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQUploadView_.Building_Unit,buildingUnits));
      
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
       System.out.println("final "+finalSpec);
		return finalSpec;
	}
	
	
}
