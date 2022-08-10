package com.ec.application.data;

import java.util.List;


public class IndentInventoryResponse {
	private String message;
	private List<IndentInventoryDto> indentInventory;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<IndentInventoryDto> getIndentInventory() {
		return indentInventory;
	}
	public void setIndentInventory(List<IndentInventoryDto> indentInventory) {
		this.indentInventory = indentInventory;
	}
	
}
