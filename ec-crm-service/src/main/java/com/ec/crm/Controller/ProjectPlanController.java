package com.ec.crm.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.StringNameDAO;
import com.ec.crm.Model.PropertyName;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Service.ProjectPlanService;

@RestController
@RequestMapping("/project")
public class ProjectPlanController
{

	@Autowired
	ProjectPlanService ppService;

	@GetMapping("/type")
	public List<PropertyType> getAllValues() throws Exception
	{
		return ppService.getAllPropertyTypes();
	}

	@GetMapping("/type/{id}")
	public PropertyType getSingleValue(@PathVariable Long id) throws Exception
	{
		return ppService.getSingle(id);
	}

	@PostMapping("/type")
	@ResponseStatus(HttpStatus.CREATED)
	public PropertyType createNewPropertyType(@RequestBody StringNameDAO payload) throws Exception
	{
		return ppService.createNew(payload);
	}

	@PostMapping("type/{id}/create")
	@ResponseStatus(HttpStatus.CREATED)
	public PropertyType createNewBuilding(@PathVariable Long id, @RequestBody StringNameDAO payload) throws Exception
	{
		return ppService.addBuilding(id, payload);
	}

	@PutMapping("property/{id}")
	@ResponseStatus(HttpStatus.OK)
	public PropertyName ceditBuilding(@PathVariable Long id, @RequestBody StringNameDAO payload) throws Exception
	{
		return ppService.aEditBuilding(id, payload);
	}

	@DeleteMapping("property/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> deleteBuilding(@PathVariable Long id) throws Exception
	{
		ppService.deleteBuilding(id);
		return ResponseEntity.ok("Property Deleted sucessfully.");
	}

	@PutMapping("/type/{id}")
	public PropertyType editExistingPropertyType(@RequestBody StringNameDAO payload, @PathVariable Long id)
			throws Exception
	{
		return ppService.editExisting(id, payload);
	}

	@DeleteMapping("/type/{id}")
	public ResponseEntity<String> deleteExistingPropertyType(@PathVariable Long id) throws Exception
	{
		ppService.deletePropertyType(id);
		return ResponseEntity.ok("Source Deleted sucessfully.");
	}

}
