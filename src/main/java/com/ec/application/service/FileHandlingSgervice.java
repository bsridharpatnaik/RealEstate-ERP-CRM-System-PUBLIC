package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.ec.application.model.DBFile;
import com.ec.application.model.FileInformation;

@Service
public class FileHandlingSgervice 
{
	@Autowired
	DBFileStorageService dbFileStorageService;
	
	public FileInformation uploadDoc(MultipartFile file) throws Exception 
	{
		try
		{
			FileInformation fileUploadSuccessData = new FileInformation();
			DBFile dbFile = dbFileStorageService.storeFile(file);
			fileUploadSuccessData.setFileUUId(dbFile.getId());
			fileUploadSuccessData.setFileName(dbFile.getFileName());
			return fileUploadSuccessData;
		}
		catch(MaxUploadSizeExceededException e)
		{
			throw new Exception("File size too large. Max allowed size - 15 MB");
		}
		catch(Exception e)
		{
			throw new Exception("Error uploading file.");
		}
	}

	public ResponseEntity<Resource> downloadFile(String fileId) throws Exception 
	{
		try
		{
			DBFile dbFile = dbFileStorageService.getFile(fileId);
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(dbFile.getFileType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dbFile.getFileName() + "\"")
				.body(new ByteArrayResource(dbFile.getData()));
		}	
		catch(Exception e)
		{
			throw new Exception("Error downloading file");
		}
	}

}
