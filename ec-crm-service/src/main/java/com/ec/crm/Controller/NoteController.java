package com.ec.crm.Controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.AllNotesForLeadDAO;
import com.ec.crm.Data.NoteCreateData;
import com.ec.crm.Model.Note;
import com.ec.crm.Service.NoteService;

@RestController
@RequestMapping(value="/notes",produces = { "application/json", "text/json" })
public class NoteController {
	@Autowired
	NoteService noteService;
	
	@GetMapping("/{id}")
	public Note findNoteByID(@PathVariable long id) throws Exception 
	{
		return noteService.findSingleNote(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Note createNote(@Valid @RequestBody NoteCreateData payload) throws Exception{
		
		return noteService.createNote(payload);
	}
	
	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteNote(@PathVariable Long id) throws Exception
	{
		noteService.deleteNote(id);
	}
	
	@PatchMapping("/{id}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void  moveNoteToUnpinned(@PathVariable Long id) throws Exception 
	{
		noteService.moveNoteToUnpinned(id);
	}
	
	@PutMapping("/{id}")
	public Note updateNote(@PathVariable Long id, @RequestBody NoteCreateData payload) throws Exception 
	{
		return noteService.updateNote(id, payload);
	} 
	
	@GetMapping("/all/{id}")
	public AllNotesForLeadDAO getAllNotesForLead(@PathVariable long id) throws Exception 
	{
		return noteService.getAllNotesForLead(id);
	}
}
