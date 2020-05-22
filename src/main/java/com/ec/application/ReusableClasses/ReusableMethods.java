package com.ec.application.ReusableClasses;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.ec.application.data.FileInformationDAO;
import com.ec.application.model.FileInformation;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	public static <T> String convertObjectToJson(T object)
	{
		ObjectMapper mapper = new ObjectMapper();
		try 
		{

            String jsonString = mapper.writeValueAsString(object);
            //String jsonInString2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            return jsonString;
		} 
		catch (IOException e) 
		{
            e.printStackTrace();
            return null;
        }
		
	}
	
	public static <T> T convertJSONtoObject(String json, Class c)
	{
		ObjectMapper mapper = new ObjectMapper();
		T t=null;
		try 
		{

			T object = (T) mapper.readValue(json,c );
			return object;
        } 
		catch (IOException e) 
		{
            e.printStackTrace();
            return null;
        }
	}
	public static String convertUTCToIST(Date date)
	{
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	    sdf.setTimeZone(TimeZone.getTimeZone("IST"));
	    return sdf.format(date);
	}
}