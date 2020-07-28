package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadStatusEnum;
import com.ec.crm.Model.Note;

public class LeadDetailInfo {
	Lead LeadDetails;
	List<Note> PinnedNotes;
	List<Note> UnpinnedNotes;
	LeadStatusEnum StatusInfo;
	List<LeadStatusEnum> HistoricalStatus;
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
	public LeadStatusEnum getStatusInfo() {
		return StatusInfo;
	}
	public void setStatusInfo(LeadStatusEnum statusInfo) {
		StatusInfo = statusInfo;
	}
	public List<LeadStatusEnum> getHistoricalStatus() {
		return HistoricalStatus;
	}
	public void setHistoricalStatus(List<LeadStatusEnum> historicalStatus) {
		HistoricalStatus = historicalStatus;
	}	
}
