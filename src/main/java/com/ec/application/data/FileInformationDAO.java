package com.ec.application.data;

import org.springframework.lang.NonNull;

public class FileInformationDAO 
{
	//@NonNull
	String fileUUId;
	//@NonNull
	String fileName;
	public String getFileUUId() {
		return fileUUId;
	}
	public void setFileUUId(String fileUUId) {
		this.fileUUId = fileUUId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
