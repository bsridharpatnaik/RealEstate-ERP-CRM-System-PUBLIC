package com.ec.application.ReusableClasses;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.ec.application.data.FileInformationDAO;
import com.ec.application.model.FileInformation;

public final class ReusableMethods 
{
	
	public static List<String> removeNullsFromStringList(List<String> itemList)
	{
		while(itemList.remove(null)) {}
		return itemList;
	}	
	
	public static <T> List<T> convertSetToList(Set<T> set) 
    { 
        // create an empty list 
        List<T> list = new ArrayList<>(); 
  
        // push each element in the set into the list 
        for (T t : set) 
            list.add(t); 
  
        // return the list 
        return list; 
    }

	public static Set<FileInformation> convertFilesListToSet(List<FileInformationDAO> fileInformationsDAO) 
	{
		Set<FileInformation> fileSet = new HashSet<>();
		for(FileInformationDAO fileInformationDAO : fileInformationsDAO)
		{
			FileInformation fileInformation = new FileInformation();
			fileInformation.setFileName(fileInformationDAO.getFileName());
			fileInformation.setFileUUId(fileInformationDAO.getFileUUId());
			fileSet.add(fileInformation);
		}
		return fileSet;
	}
	
	public static String convertUTCToIST(Date date)
	{
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	    sdf.setTimeZone(TimeZone.getTimeZone("IST"));
	    return sdf.format(date);
	}
}