package com.ec.application.data;

import java.sql.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class OutwardInventoryData 
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	Date date;
	
	@NonNull
	Long contractorId;
	
	@NonNull
	Long usageLocationId;
	
	@NonNull
	Long warehouseId;
	
	String slipNo;
	
	@NonNull
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String purpose;
	
	@NonNull
	Long  usageAreaId;
	
	@NonNull
	List<ProductWithQuantity> productWithQuantities;
	
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String additionalInfo;

	@NonNull
	List<FileInformationDAO> fileInformations;
}
