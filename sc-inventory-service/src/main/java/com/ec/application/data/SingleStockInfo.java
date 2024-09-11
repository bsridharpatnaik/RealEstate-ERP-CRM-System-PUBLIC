package com.ec.application.data;

import java.util.Date;
import java.util.List;

import com.ec.application.model.Stock;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class SingleStockInfo
{

	Long productId;
	String productName;
	String categoryName;
	Double reorderQuantity;
	String totalQuantityInHand;
	String stockStatus;
	List<Stock> detailedStock;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted to match DATE type
	Date lastInwardDate;
}
