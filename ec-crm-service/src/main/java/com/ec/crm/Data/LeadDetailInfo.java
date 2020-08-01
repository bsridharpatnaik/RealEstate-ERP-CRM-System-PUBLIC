package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;

import lombok.Data;

@Data
public class LeadDetailInfo {
	
	AllNotesForLeadDAO allNotes;
}
