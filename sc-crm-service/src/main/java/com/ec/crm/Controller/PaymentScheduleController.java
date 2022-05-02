package com.ec.crm.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Data.PaymentScheduleListingDTO;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Service.PaymentScheduleService;

@RestController
@RequestMapping("/customer/payments/scheduled")
public class PaymentScheduleController
{

	@Autowired
	PaymentScheduleService psService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public Page<PaymentScheduleListingDTO> returnFilteredPaymentSchedules(
			@RequestBody FilterDataList leadFilterDataList,
			@PageableDefault(page = 0, size = 10, sort = "scheduleId", direction = Direction.DESC) Pageable pageable)
			throws Exception
	{
		return psService.findFilteredDataForPayments(leadFilterDataList, pageable);
	}

	@GetMapping("/dropdown")
	@ResponseStatus(HttpStatus.OK)
	public DropdownForClosedLeads getDropDownValues() throws Exception
	{
		return psService.getDropDownValues();
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex)
	{
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) ->
		{
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return errors;
	}

}
