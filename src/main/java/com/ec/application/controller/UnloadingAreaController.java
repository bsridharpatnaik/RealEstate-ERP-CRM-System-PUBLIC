package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.BasicEntities.UnloadingArea;
import com.ec.application.service.UnloadingAreaService;

@RestController
@RequestMapping("/unloadingarea")
public class UnloadingAreaController 
{

	@Autowired
	UnloadingAreaService UnloadingAreaService;
	
	@GetMapping
	public Page<UnloadingArea> returnAllPayments(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return UnloadingAreaService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public UnloadingArea findUnloadingAreabyvehicleNoUnloadingAreas(@PathVariable long id) 
	{
		return UnloadingAreaService.findSingleUnloadingArea(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteUnloadingArea(@PathVariable Long id) throws Exception
	{
		UnloadingAreaService.deleteUnloadingArea(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public UnloadingArea createUnloadingArea(@RequestBody UnloadingArea payload) throws Exception{
		
		return UnloadingAreaService.createUnloadingArea(payload);
	}

	@PutMapping("/{id}")
	public UnloadingArea updateUnloadingArea(@PathVariable Long id, @RequestBody UnloadingArea UnloadingArea) throws Exception 
	{
		return UnloadingAreaService.updateUnloadingArea(id, UnloadingArea);
	} 
	
	@GetMapping("/name/{name}")
	public ArrayList<UnloadingArea> returnCusByName(@PathVariable String name) 
	{
		return UnloadingAreaService.findUnloadingAreasByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<UnloadingArea> returnCusByPartialName(@PathVariable String name) 
	{
		return UnloadingAreaService.findUnloadingAreasByPartialName(name);
	}
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return UnloadingAreaService.findIdAndNames();
	}
}
