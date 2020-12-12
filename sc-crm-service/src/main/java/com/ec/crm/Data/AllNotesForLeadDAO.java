package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Model.Note;

import lombok.Data;

@Data
public class AllNotesForLeadDAO 
{
	List<Note> pinnedNotes;
	List<Note> unPinnedNotes;
}
