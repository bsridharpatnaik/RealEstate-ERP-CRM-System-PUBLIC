package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.Model.StatusEnum;

public class LeadDetailInfo {
	Lead LeadDetails;
	List<Note> PinnedNotes;
	List<Note> UnpinnedNotes;
	StatusEnum StatusInfo;
	List<StatusEnum> HistoricalStatus;
	public Lead getLeadDetails() {
		return LeadDetails;
	}
	public void setLeadDetails(Lead leadDetails) {
		LeadDetails = leadDetails;
	}
	public List<Note> getPinnedNotes() {
		return PinnedNotes;
	}
	public void setPinnedNotes(List<Note> pinnedNotes) {
		PinnedNotes = pinnedNotes;
	}
	public List<Note> getUnpinnedNotes() {
		return UnpinnedNotes;
	}
	public void setUnpinnedNotes(List<Note> unpinnedNotes) {
		UnpinnedNotes = unpinnedNotes;
	}
	public StatusEnum getStatusInfo() {
		return StatusInfo;
	}
	public void setStatusInfo(StatusEnum statusInfo) {
		StatusInfo = statusInfo;
	}
	public List<StatusEnum> getHistoricalStatus() {
		return HistoricalStatus;
	}
	public void setHistoricalStatus(List<StatusEnum> historicalStatus) {
		HistoricalStatus = historicalStatus;
	}	
}
