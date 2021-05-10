package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.AllNotesForLeadDAO;
import com.ec.crm.Data.NoteCreateData;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NoteRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class NoteService 
{
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	NoteRepo nRepo;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	Logger log = LoggerFactory.getLogger(NoteService.class);
	
	Long currentUserID;

	public Note createNote(@Valid NoteCreateData payload) throws Exception 
	{
		log.info("Invoked createNote");
		log.info("Validating Payload");
		validatePayload(payload);
		log.info("Checking if lead exist with leadId");
		log.info("Getting current user");
		currentUserID = userDetailsService.getCurrentUser().getId();
		Note note=new Note();
		setFields(note,payload,"create");
		nRepo.save(note);
		return note;
	}
	
	public Note updateNote(Long id, NoteCreateData payload) throws Exception 
	{
		log.info("Invoked updateNote");
		validatePayload(payload);
		
		Optional<Note> noteOpt = nRepo.findById(id);
		if(! noteOpt.isPresent())
			throw new Exception("Note with ID not found");
		
		Note note=noteOpt.get();
		if(note.getPinned()==false)
			throw new Exception("Cannot edit unpinned note");
		
		setFields(note, payload, "update");
		nRepo.save(note);
		return note;
	}
	
	public AllNotesForLeadDAO getAllNotesForLead(long id)
	{
		AllNotesForLeadDAO AllNotesForLeadDAO = new AllNotesForLeadDAO();
		AllNotesForLeadDAO.setPinnedNotes(nRepo.findNotesForLead(id,true));
		AllNotesForLeadDAO.setUnPinnedNotes(nRepo.findNotesForLead(id,false));
		return AllNotesForLeadDAO;
	}
	public Note findSingleNote(long id) throws Exception 
	{
		log.info("Invoked findSingleNote");
		Optional<Note> note = nRepo.findById(id);
		if(note.isPresent())
			return note.get();
		else
			throw new Exception("Note ID not found");
	}
	
	public void moveNoteToUnpinned(Long id) throws Exception 
	{
		log.info("Invoked setPinnedForNote");
		
		Optional<Note> noteOpt = nRepo.findById(id);
		if(! noteOpt.isPresent())
			throw new Exception("Note with ID not found");
		
		Note note=noteOpt.get();
		note.setPinned(false);
		nRepo.save(note);
	}
	
	
	private void setFields(Note note, @Valid NoteCreateData payload,String type) 
	{
		log.info("setFields setFields");
		note.setContent(payload.getContent());
		note.setPinned(payload.getPinned()!=null?payload.getPinned():false);
		note.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));
		if(type.equals("create"))
		{
			note.setCreatorId(currentUserID);
			note.setLead(lRepo.findById(payload.getLeadId()).get());
		}
	}

	private void validatePayload(@Valid NoteCreateData payload) throws Exception 
	{
		log.info("setFields validatePayload");
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		if(! leadOpt.isPresent())
			throw new Exception("Lead not found");
		if(payload.getContent().length()>320)
			throw new Exception("Please enter note with length less than 320 characters");
	}

	public void deleteNote(Long id) throws Exception 
	{
		log.info("setFields deleteNote");
		Optional<Note> noteOpt = nRepo.findById(id);
		if(! noteOpt.isPresent())
			throw new Exception("Note with ID not found");
		Note note = noteOpt.get();
		note.setPinned(false);
		nRepo.save(note);
	}
}
