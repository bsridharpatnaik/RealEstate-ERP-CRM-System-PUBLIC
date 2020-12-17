package com.ec.application.service;

import java.io.IOException;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ec.application.model.DBFile;
import com.ec.application.repository.DBFileRepository;

@Service
@Transactional
public class DBFileStorageService
{

	@Autowired
	private DBFileRepository dbFileRepository;

	public DBFile storeFile(MultipartFile file)
	{
		// Normalize file name
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());

		try
		{
			// Check if the file's name contains invalid characters
			if (fileName.contains(".."))
			{
				// throw new FileStorageException("Sorry! Filename contains invalid path
				// sequence " + fileName);
			}

			DBFile dbFile = new DBFile(fileName, file.getContentType(), file.getBytes());

			return dbFileRepository.save(dbFile);
		} catch (IOException ex)
		{
			// throw new FileStorageException("Could not store file " + fileName + ". Please
			// try again!", ex);
		}
		return null;
	}

	public DBFile getFile(String fileId) throws Exception
	{
		return dbFileRepository.findById(fileId).orElseThrow(() -> new Exception("File not found with id " + fileId));
	}
}