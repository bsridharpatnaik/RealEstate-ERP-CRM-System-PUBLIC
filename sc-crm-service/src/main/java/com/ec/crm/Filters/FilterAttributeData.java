package com.ec.crm.Filters;

import java.util.List;

public class FilterAttributeData {
	
	private String attrName;
	private List<String> attrValue;
	public String getAttrName() {
		return attrName;
	}
	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}
	public List<String> getAttrValue() {
		return attrValue;
	}
	public void setAttrValue(List<String> attrValue) {
		this.attrValue = attrValue;
	}
	@Override
	public String toString() {
		return "FilterAttributeData [attrName=" + attrName + ", attrValue=" + attrValue + ", getAttrName()="
				+ getAttrName() + ", getAttrValue()=" + getAttrValue() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}
	
}
