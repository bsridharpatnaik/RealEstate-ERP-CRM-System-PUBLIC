package com.ec.common.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.ContactsWithTypeAhead;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Service.CheckBeforeDeleteService;
import com.ec.common.Service.ContactService;

@RestController
@RequestMapping(value="/contact",produces = { "application/json", "text/json" })
public class ContactController 
{

	@Autowired
	ContactService contactService;
	

	@GetMapping
	public ContactsWithTypeAhead returnAllContacts(@RequestParam(name="page",required = false) Integer page,@PageableDefault(page = 0, size = 10, sort = "contactId", direction = Direction.DESC) Pageable pageable) 
	{
		return contactService.findAllWithTypeAhead(pageable);
	}
	
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public ContactsWithTypeAhead returnFilteredContacts(@RequestBody FilterDataList contactFilterDataList,@PageableDefault(page = 0, size = 10, sort = "contactId", direction = Direction.DESC) Pageable pageable) 
	{
		return contactService.findFilteredContactsWithTA(contactFilterDataList,pageable);
	}
	
	@GetMapping("/{id}")
	public ContactAllInfo findContactbyvehicleNoContacts(@PathVariable long id) throws Exception 
	{
		return contactService.findSingleContactFromAll(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteContact(@PathVariable Long id) throws Exception
	{
		//try
		//{
			contactService.deleteContact(id);
			return ResponseEntity.ok("Contact Deleted sucessfully.");
		//}
		//catch(Exception e)
		//{
		//	throw new Exception("Not able to delete contact");
		//}
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public ContactAllInfo createContact(@RequestBody ContactAllInfo payload) throws Exception
	{
		return(contactService.createContact(payload));
	}

	@PutMapping("/{id}")
	public ContactAllInfo updateContact(@PathVariable Long id, @RequestBody ContactAllInfo payload) throws Exception 
	{
		return contactService.updateContact(id, payload);
	} 
}
