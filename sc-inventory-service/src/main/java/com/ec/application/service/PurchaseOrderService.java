package com.ec.application.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.FilterIndentAttributeData;
import com.ec.application.data.IndentInventoryDto;
import com.ec.application.data.IndentInventoryResponse;
import com.ec.application.data.IndentResponse;
import com.ec.application.model.Indent;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.Product;
import com.ec.application.repository.IndentInventoryRepository;
import com.ec.application.repository.IndentRepository;
import com.ec.application.repository.ProductRepo;
import com.ec.common.Filters.FilterAttributeData;

@Service
public class PurchaseOrderService {

	@Autowired
	private IndentInventoryRepository indentInventoryRepository;
	
	@Autowired
	private IndentRepository indentRepository;
	
	@Autowired
	private ProductRepo productRepository;
	
	Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);
	
	public IndentResponse addIndent(List<IndentInventoryDto> listIndentInventory) {
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		IndentResponse indentResponse=new IndentResponse();
		
        try
        {
        	
        	Indent indent=new Indent();
        	indent.setCreationDate(new Timestamp(System.currentTimeMillis()));
        	indent.setIndentStatus("Created");
        	indent.setNoOfInventory(listIndentInventory.size());
        	Indent indentId = indentRepository.save(indent);
        	
        	for(IndentInventoryDto iterateInventory:listIndentInventory)
        	{
        		
        		Product product=productRepository.findByProductId(iterateInventory.getInventory());
        		IndentInventory indentInventory=new IndentInventory();	
        		indentInventory.setIndent(indentId);
        		indentInventory.setProduct(product);
        		indentInventory.setQuantity(iterateInventory.getQuantity());
        		indentInventory.setStock(iterateInventory.getStock());
        		indentInventory.setUnit(iterateInventory.getUnit());
        		indentInventoryRepository.save(indentInventory);
        	}
        	
        	indentResponse.setMessage("Add inventory successfully");
        }
        catch (Exception e) {
			e.printStackTrace();
			
		}
		return indentResponse;
	}

	public IndentResponse getListOfIndent() {
		IndentResponse indentResponse=new IndentResponse();
        try
        {
        	List<Indent> indent=indentRepository.findAll();
        	indentResponse.setIndent(indent);
        	indentResponse.setMessage("Get list of indent successfully");
        }
        catch (Exception e) {
			e.printStackTrace();
		}
		return indentResponse;
	}

	public IndentInventoryResponse viewIndent(long indentNumber) {
		IndentInventoryResponse indentInventoryResponse=new IndentInventoryResponse();
		try
		{
			
			List<IndentInventory> listOfindentInventory=indentInventoryRepository.findByIndentIndentNumber(indentNumber);
			System.out.println("hello");
			List<IndentInventoryDto> listOfIndentInventoryDto=new ArrayList<>();
			for(IndentInventory indentInventory: listOfindentInventory)
			{
			
				IndentInventoryDto indentInventoryDto=new IndentInventoryDto();
				indentInventoryDto.setId(indentInventory.getId());
				indentInventoryDto.setInventoryName(indentInventory.getProduct().getProductName());
				indentInventoryDto.setQuantity(indentInventory.getQuantity());
				indentInventoryDto.setStock(indentInventory.getStock());
				indentInventoryDto.setUnit(indentInventory.getUnit());
				indentInventoryDto.setInventory(indentInventory.getProduct().getProductId());
				listOfIndentInventoryDto.add(indentInventoryDto);
			}
			indentInventoryResponse.setIndentInventory(listOfIndentInventoryDto);
			indentInventoryResponse.setMessage("Get Indent Successuflly");
			
		}
		catch (Exception e) {
			e.printStackTrace();
			indentInventoryResponse.setMessage("Get Indent failed");
		}
		return indentInventoryResponse;
	}

	public IndentInventoryResponse updateIndent(List<IndentInventoryDto> listIndentInventory) {
		IndentInventoryResponse indentInventoryResponse=new IndentInventoryResponse();
		try
		{			
			for(IndentInventoryDto iterateInventory:listIndentInventory)
        	{
				IndentInventory indentInventory=indentInventoryRepository.findById(iterateInventory.getId());
        		Product product=productRepository.findByProductId(iterateInventory.getInventory());
        		
        		System.out.println(product.getProductId());
        		indentInventory.setProduct(product);
        		indentInventory.setQuantity(iterateInventory.getQuantity());
        		indentInventory.setStock(iterateInventory.getStock());
        		indentInventory.setUnit(iterateInventory.getUnit());
        		indentInventoryRepository.save(indentInventory);
        		
        	}
        	
			indentInventoryResponse.setMessage("Update Indent Successuflly");
		}
		catch (Exception e) {
			e.printStackTrace();
			indentInventoryResponse.setMessage("Update Indent failed");
		}
		return indentInventoryResponse;
	}

	
	public IndentInventoryResponse deleteIndent(long indentNumber) {
		IndentInventoryResponse indentInventoryResponse=new IndentInventoryResponse();
		try
		{
			
			List<IndentInventory> indentInventory=indentInventoryRepository.findByIndentIndentNumber(indentNumber);
			if(!indentInventory.isEmpty())
			{
				indentInventoryRepository.deleteByIndentIndentNumber(indentNumber);
			}
			
			Indent indent=indentRepository.findByIndentNumber(indentNumber);
			if(indent!=null)
			{
				indentRepository.deleteByIndentNumber(indentNumber);
			}
			indentInventoryResponse.setMessage("Delete Indent successfully");
		}
		catch (Exception e) {
			e.printStackTrace();
			indentInventoryResponse.setMessage("Delete Indent failed");
		}
		return indentInventoryResponse;
	}

	

	public IndentResponse searchByIndentNumber(FilterIndentAttributeData filterAttributeData) {
		IndentResponse indentResponse=new IndentResponse();
		try
		{
			List<Indent> list=new ArrayList<>();
			List<Indent> indent=indentRepository.findByIndentNumberIn(filterAttributeData.getAttrValue());
			if(!indent.isEmpty())
			{
				list.addAll(indent);	
			}
			indentResponse.setIndent(indent);
			indentResponse.setMessage("Successfully done");
		}
		catch (Exception e) {
			e.printStackTrace();
			indentResponse.setMessage("Successfully failed");
		}
		return indentResponse;
	}

     public List<IdNameProjections> getListOfIndentNumber() {
		
		return indentRepository.findIdAndNames();
	 }

	public List<IdNameProjections> productMeasurementUnit(long productId) {
			log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return productRepository.findIdAndMeasurementUnitNames(productId);
    }
	
}
