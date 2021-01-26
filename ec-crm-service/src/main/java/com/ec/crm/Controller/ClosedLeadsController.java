package com.ec.crm.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.ApiOnlyMessageAndCodeError;
import com.ec.crm.Data.ClosedLeadsListDTO;
import com.ec.crm.Data.CreateCustomerDocumentDTO;
import com.ec.crm.Data.CreateDealStructureDTO;
import com.ec.crm.Data.CreateScheduleData;
import com.ec.crm.Data.CustomerDetailInfo;
import com.ec.crm.Data.DealStructureDAO;
import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Data.LeadDAO;
import com.ec.crm.Data.ScheduleReturnDAO;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.CustomerDocument;
import com.ec.crm.ReusableClasses.IdNameProjections;
//import com.ec.crm.Model.DealStructure;
import com.ec.crm.Service.ClosedLeadService;
import com.ec.crm.Service.CustomerDocumentsService;
import com.ec.crm.Service.DealStructureService;
import com.ec.crm.Service.PaymentScheduleService;

@RestController
@RequestMapping(value = "/customer", produces =
{ "application/json", "text/json" })
public class ClosedLeadsController
{
	@Autowired
	ClosedLeadService clService;

	@Autowired
	DealStructureService dsService;

	@Autowired
	CustomerDocumentsService cdService;

	@Autowired
	PaymentScheduleService psService;

	@PostMapping
	public Page<ClosedLeadsListDTO> returnAllCustomer(
			@PageableDefault(page = 0, size = 10, sort = "leadId", direction = Direction.DESC) Pageable pageable,
			@RequestBody FilterDataList filterDataList) throws Exception
	{
		return clService.fetchAllClosedLeads(pageable, filterDataList);
	}

	@GetMapping("/dropdown")
	public DropdownForClosedLeads getDropdownValues() throws Exception
	{
		return clService.getDropDownValues();
	}

	@GetMapping("/customerdetails/{id}")
	public LeadDAO getCustomerDetailedInfo(@PathVariable Long id) throws Exception
	{
		return clService.getCustomerDetails(id);
	}

	@GetMapping("/activities/{id}")
	public CustomerDetailInfo getCustomerActivities(@PathVariable Long id) throws Exception
	{
		return clService.getCustomerDetailedInfo(id);
	}

	/*
	 * @PostMapping("/dealstructure") public DealStructure
	 * createDealStructure(CreateDealStructureDTO payload) throws Exception { return
	 * dsService.updateDealStructure(payload); }
	 */

	@PostMapping("/document/create")
	public CustomerDocument createDocument(@RequestBody CreateCustomerDocumentDTO payload) throws Exception
	{
		return cdService.createDocument(payload);
	}

	@GetMapping("/{id}/documents")
	public List<CustomerDocument> fetchDocuments(@PathVariable Long id) throws Exception
	{
		return cdService.fetchDocumentsForLead(id);
	}

	@PutMapping("/documents/{id}")
	public CustomerDocument updateCustomerDocument(@PathVariable Long id,
			@RequestBody CreateCustomerDocumentDTO payload) throws Exception
	{
		return cdService.updateDocument(id, payload);
	}

	@GetMapping("/documents/{id}")
	public CustomerDocument getCustomerDocument(@PathVariable Long id) throws Exception
	{
		return cdService.getById(id);
	}

	// Deal Structure
	@GetMapping("/dealstructure/propertytypes")
	public List<IdNameProjections> getPropertyTypeList() throws Exception
	{
		return dsService.getPropertyTypes();
	}

	@GetMapping("/dealstructure/bycustomer/{id}")
	public List<DealStructureDAO> getDealStructresForLead(@PathVariable Long id) throws Exception
	{
		return dsService.getDealStructuresForLead(id);
	}

	@GetMapping("/dealstructure/{id}/properties")
	public List<IdNameProjections> getPropertyList(@PathVariable Long id) throws Exception
	{
		return dsService.getPropertiesList(id);
	}

	@PostMapping("/dealstructure/create")
	public DealStructureDAO createDealStructure(@RequestBody CreateDealStructureDTO payload) throws Exception
	{
		return dsService.createDealStructure(payload);
	}

	@PutMapping("/dealstructure/{id}")
	public DealStructureDAO updateDealStructure(@RequestBody CreateDealStructureDTO payload, @PathVariable Long id)
			throws Exception
	{
		return dsService.updateDealStructure(payload, id);
	}

	@GetMapping("/dealstructure/{id}")
	public DealStructureDAO getDealStructure(@PathVariable Long id) throws Exception
	{
		return dsService.getDealStructure(id);
	}

	@DeleteMapping("/dealstructure/{id}")
	public ResponseEntity<?> deleteDealStructure(@PathVariable Long id) throws Exception
	{
		dsService.deleteDealStructure(id);
		return ResponseEntity.ok("Deal Struture Deleted sucessfully.");
	}

	// Payment Structure

	@PostMapping("/paymentschedule/create")
	public ScheduleReturnDAO createPaymentSchedule(@RequestBody CreateScheduleData payload) throws Exception
	{
		return psService.createSchedule(payload);
	}

	@PutMapping("/paymentschedule/{id}")
	public ScheduleReturnDAO updateDPaymentSchedule(@RequestBody CreateScheduleData payload, @PathVariable Long id)
			throws Exception
	{
		return psService.updateSchedule(payload, id);
	}

	@GetMapping("/paymentschedule/{id}")
	public ScheduleReturnDAO getPaymentSchedule(@PathVariable Long id) throws Exception
	{

		return psService.getPaymentSchedule(id);
	}

	@DeleteMapping("/paymentschedule/{id}")
	public ResponseEntity<?> deletePaymentSchedule(@PathVariable Long id) throws Exception
	{
		psService.deletePaymentSchedule(id);
		return ResponseEntity.ok("Payment Schedule Deleted sucessfully.");
	}

	@GetMapping("/paymentschedule/bydealid/{id}")
	public List<ScheduleReturnDAO> getPaymentScheduleForDeal(@PathVariable Long id) throws Exception
	{

		return psService.getSchedulesForDeal(id);
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
