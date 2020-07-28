package com.ec.crm.Data;

import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.crm.Model.Broker;

public class NoteCreateData {
	
	String content;
	
	@NonNull
	Long leadId;
	
	Boolean pinned;
	
	@NonNull
	List<FileInformationDAO> fileInformations;
	
	@Override
	public String toString() {
		return "NoteCreateData [content=" + content + ", leadId=" + leadId + ", pinned=" + pinned
				+ ", fileInformations=" + fileInformations + "]";
	}

	public List<FileInformationDAO> getFileInformations() {
		return fileInformations;
	}

	public void setFileInformations(List<FileInformationDAO> fileInformations) {
		this.fileInformations = fileInformations;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getLeadId() {
		return leadId;
	}

	public void setLeadId(Long leadId) {
		this.leadId = leadId;
	}

	public Boolean getPinned() {
		return pinned;
	}

	public void setPinned(Boolean pinned) {
		this.pinned = pinned;
	}
}
