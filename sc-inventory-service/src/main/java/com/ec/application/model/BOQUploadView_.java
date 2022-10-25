package com.ec.application.model;

import javax.persistence.metamodel.SingularAttribute;

import com.ec.application.data.BOQStatusView;

public abstract class BOQUploadView_ {

	public static volatile SingularAttribute<BOQStatusView, String> buildingType;
	public static volatile SingularAttribute<BOQStatusView, String> buildingUnit;
	public static volatile SingularAttribute<BOQStatusView, String> product;
	public static volatile SingularAttribute<BOQStatusView, String> category;
	public static volatile SingularAttribute<BOQStatusView, String> status;


	public static final String Building_Type = "buildingType";
	public static final String Building_Unit = "buildingUnit";
	public static final String Product = "product";
	public static final String Category = "category";
	public static final String Status = "status";

	

}
