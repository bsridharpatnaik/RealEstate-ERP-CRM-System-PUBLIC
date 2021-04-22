package com.ec.application.controller;

import java.util.List;

import com.ec.application.data.*;
import com.ec.application.service.StockInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.service.StockService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/stock")
public class StockController
{
	@Autowired
	StockService stockService;

	@PostMapping(produces= MediaType.APPLICATION_JSON_VALUE)
	public StockInformationV2 getAllStocks(
			@PageableDefault(page = 0, size = 10, sort = "productName", direction = Direction.ASC) Pageable pageable,
			@RequestBody FilterDataList filterDataList)
	{
		return stockService.fetchStockInformation(pageable,filterDataList);
	}

	@PostMapping("/export")
	@ResponseStatus(HttpStatus.OK)
	public List<StockInformationExportDAO> returnAllStockForExport(@RequestBody FilterDataList filterDataList)
			throws Exception
	{
		return stockService.findStockForAllForExport(filterDataList);
	}

	@PostMapping("/current")
	public List<ProductIdAndStockProjection> returnStockForProductWarehouse(
			@RequestBody CurrentStockRequest currentStockRequest)
	{
		return stockService.findStockForProductListWarehouse(currentStockRequest);
	}

	@GetMapping("/current")
	public double getStockForProductWarehouse(@RequestParam Long productId, @RequestParam Long warehouseId)
	{
		Double currentStock;
		currentStock = stockService.findStockForProductWarehouse(productId, warehouseId);
		return currentStock == null ? 0 : currentStock;
	}

	@GetMapping("/getfilterdropdown")
	public NameAndProjectionDataForDropDown getDropdownValuesForFilter()
	{

		return stockService.getStockDropdownValues();
	}

	@ExceptionHandler(
	{ JpaSystemException.class })
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiOnlyMessageAndCodeError sqlError(Exception ex)
	{
		ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
				"Something went wrong while handling data. Contact Administrator.");
		return apiError;
	}
}
