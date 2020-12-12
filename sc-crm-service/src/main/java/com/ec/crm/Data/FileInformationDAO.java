package com.ec.crm.Data;

import org.springframework.lang.NonNull;

import lombok.Data;
@Data
public class FileInformationDAO 
{
	@NonNull
	String fileUUId;
	@NonNull
	String fileName;
}
