package com.ec.application.data;

import lombok.Data;

@Data
public class BOQStatusTypeListWithConsumedUnitCount
{
	Long typeId;
	String typeName;
	Long boqCrossedUnitCount;
	Long totalUnitCount;
}
