package com.ec.application.data;

import java.util.List;

import com.ec.application.model.Indent;

public class IndentResponse {

	private String message;
	private List<Indent> indent;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<Indent> getIndent() {
		return indent;
	}

	public void setIndent(List<Indent> indent) {
		this.indent = indent;
	}
}
