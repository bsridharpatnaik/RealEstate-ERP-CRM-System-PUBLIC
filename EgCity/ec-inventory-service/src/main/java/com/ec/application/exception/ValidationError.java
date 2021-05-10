package com.ec.application.exception;

public class ValidationError {

	private String errorCode;
	private String errorDescription;
	private String attribute;
	
	public ValidationError(String attribute,String errorCode,String errorDescription) {
		this.attribute=attribute;
		this.errorCode=errorCode;
		this.errorDescription=errorDescription;
	}
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDescription() {
		return errorDescription;
	}
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	
	
}
