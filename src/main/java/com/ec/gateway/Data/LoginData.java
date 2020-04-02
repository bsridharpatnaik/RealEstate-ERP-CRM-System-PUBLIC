package com.ec.gateway.Data;

import com.ec.gateway.Exception.ValidationError;

public class LoginData {
	
	private String token;
	private String message;
	private String expiryTime;
	private boolean status;
	
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getExpiryTime() {
		return expiryTime;
	}
	public void setExpiryTime(String expiryTime) {
		this.expiryTime = expiryTime;
	}
	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	public void addError(String string, ValidationError validationError) {

		
	}
	
}
