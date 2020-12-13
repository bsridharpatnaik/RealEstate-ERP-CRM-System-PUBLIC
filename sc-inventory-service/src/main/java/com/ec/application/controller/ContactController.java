package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
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

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.model.Contact;
import com.ec.application.service.ContactService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/contact")
public class ContactController
{
	@Autowired
	ContactService contactInfoService;

	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	public Contact createCategory(@RequestBody Contact payload) throws Exception
	{

		return contactInfoService.createContact(payload);
	}

	@GetMapping("/{id}")
	public Contact findContactbyvehicleNoContacts(@PathVariable long id) throws Exception
	{
		return contactInfoService.findContactById(id);
	}

	@GetMapping("typeahead/globalsearch/{nameorno}")
	public List<String> findTypeAhead(@PathVariable String nameorno) throws Exception
	{
		return contactInfoService.typeAheadForSearch(nameorno);
	}

	@GetMapping("typeahead/namesearch/{name}")
	public List<String> findTypeAheadForName(@PathVariable String name) throws Exception
	{
		return contactInfoService.typeAheadForName(name);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public Page<Contact> returnFilteredContacts(@RequestBody FilterDataList contactFilterDataList,
			@PageableDefault(page = 0, size = 10, sort = "contactId", direction = Direction.DESC) Pageable pageable)
	{
		return contactInfoService.findFilteredContactsWithTA(contactFilterDataList, pageable);
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

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteContact(@PathVariable Long id) throws Exception
	{
		contactInfoService.deleteContact(id);
		return ResponseEntity.ok("Contact Deleted sucessfully.");
	}

	@PutMapping("/{id}")
	public Contact updateContact(@PathVariable Long id, @RequestBody Contact payload) throws Exception
	{
		return contactInfoService.updateContact(id, payload);
	}

}
