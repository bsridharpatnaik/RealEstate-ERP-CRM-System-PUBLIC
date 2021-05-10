package com.ec.crm.Data;

import lombok.Data;

@Data
public class CreateCustomerDocumentDTO
{
	Long leadId;
	String documentName;
	Boolean receivedStatus;
	FileInformationDAO fileInformation;
}
