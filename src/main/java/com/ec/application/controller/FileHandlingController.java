package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ec.application.model.FileInformation;
import com.ec.application.service.FileHandlingSgervice;

@RestController
@RequestMapping("/file")
public class FileHandlingController 
{
	@Autowired
	FileHandlingSgervice fileHandlingSgervice;
	
	@PostMapping(value = "/upload")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public FileInformation uploadDoc(@RequestParam("file") MultipartFile file,@RequestParam("type") String type,@RequestParam("id") Long id) throws Exception
	{
		return fileHandlingSgervice.uploadDoc(file, type, id);
	}

	@GetMapping("/download/{fileId}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) throws Exception 
	{
		return fileHandlingSgervice.downloadFile(fileId);
	}
}
