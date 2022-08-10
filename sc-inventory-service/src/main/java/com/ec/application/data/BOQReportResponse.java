package com.ec.application.data;

import java.util.List;


public class BOQReportResponse {
	private String message;
	private List<BOQReportDto> boqreports;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<BOQReportDto> getBoqreports() {
		return boqreports;
	}
	public void setBoqreports(List<BOQReportDto> boqreports) {
		this.boqreports = boqreports;
	}
	
}
