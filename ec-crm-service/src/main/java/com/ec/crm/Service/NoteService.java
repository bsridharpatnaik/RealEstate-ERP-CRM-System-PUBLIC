package com.ec.crm.Service;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.NoteCreateData;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NoteRepo;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class NoteService {
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	NoteRepo nRepo;
	
	public Page<Note> fetchAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return nRepo.findAll(pageable);
	}

	public Note findSingleNote(long id) throws Exception {
		// TODO Auto-generated method stub
		Optional<Note> note = nRepo.findById(id);
		if(note.isPresent())
			return note.get();
		else
			throw new Exception("Note ID not found");
	}

	public Note createNote(@Valid NoteCreateData payload) throws Exception {
		// TODO Auto-generated method stub
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		if(! leadOpt.isPresent())
			throw new Exception("Lead not found");
		
		Note note=new Note();
		note.setContent(payload.getContent());
		note.setFileId(payload.getFileId());
		note.setLead(leadOpt.get());
		note.setPinned(payload.getPinned());
		nRepo.save(note);
		return note;
	}

	public void deleteNote(Long id) {
		// TODO Auto-generated method stub
		nRepo.softDeleteById(id);
	}

	public Note updateNote(Long id, NoteCreateData payload) throws Exception {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		Optional<Note> noteOpt = nRepo.findById(id);
		if(! noteOpt.isPresent())
			throw new Exception("Note not found");
		
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		if(! leadOpt.isPresent())
			throw new Exception("Lead not found");
		

		Note note=noteOpt.get();
		note.setContent(payload.getContent());
		note.setFileId(payload.getFileId());
		note.setLead(leadOpt.get());
		note.setPinned(payload.getPinned());
		nRepo.save(note);
		return note;
	}
		
	
	
	
}
