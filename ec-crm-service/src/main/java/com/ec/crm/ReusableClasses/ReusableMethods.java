package com.ec.crm.ReusableClasses;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ec.crm.Data.FileInformationDAO;
import com.ec.crm.Model.FileInformation;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ReusableMethods
{
	public static boolean isValidMobileNumber(String s)
	{
		Pattern p = Pattern.compile("^[0][1-9]\\d{9}$|^[1-9]\\d{9}$");
		Matcher m = p.matcher(s);
		return (m.find() && m.group().equals(s));
	}

	public static boolean isValidEmail(String email)
	{
		String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
				+ "A-Z]{2,7}$";

		Pattern pat = Pattern.compile(emailRegex);
		if (email == null)
			return false;
		return pat.matcher(email).matches();
	}

	public static List<String> removeNullsFromStringList(List<String> itemList)
	{
		while (itemList.remove(null))
		{
		}
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
		for (FileInformationDAO fileInformationDAO : fileInformationsDAO)
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
			// String jsonInString2 =
			// mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
			return jsonString;
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}

	}

	public static <T> T convertJSONtoObject(String json, Class c)
	{
		ObjectMapper mapper = new ObjectMapper();
		T t = null;
		try
		{

			T object = (T) mapper.readValue(json, c);
			return object;
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static String normalizePhoneNumber(String number) throws Exception
	{
		number = number.replaceAll("[^+0-9]", ""); // All weird characters such as /, -, ...
		number = removeZero(number);
		if (number.length() < 10)
			throw new Exception("Please enter atleast 10 digit mobile number");

		number = number.substring(number.length() - 10);
		return number;
	}

	public static String removeZero(String str)
	{
		int i = 0;
		while (str.charAt(i) == '0')
			i++;
		StringBuffer sb = new StringBuffer(str);
		sb.replace(0, i, "");
		return sb.toString();
	}

	public static <T> Set<T> differenceBetweenSets(final Set<T> setOne, final Set<T> setTwo)
	{
		Set<T> result = new HashSet<T>(setOne);
		result.removeIf(setTwo::contains);
		return result;
	}

	public static <T> Set<T> commonBetweenSets(final Set<T> setOne, final Set<T> setTwo)
	{
		Set<T> result = new HashSet<T>(setOne);
		result.retainAll(setTwo);
		return result;
	}

	public static String convertUTCToIST(Date date)
	{
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		sdf.setTimeZone(TimeZone.getTimeZone("IST"));
		return sdf.format(date);
	}

	public static Date atStartOfDay(Date date)
	{
		LocalDateTime localDateTime = dateToLocalDateTime(date);
		LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
		return localDateTimeToDate(startOfDay);
	}

	public static Date atEndOfDay(Date date)
	{
		LocalDateTime localDateTime = dateToLocalDateTime(date);
		LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
		return localDateTimeToDate(endOfDay);
	}

	private static LocalDateTime dateToLocalDateTime(Date date)
	{
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	}

	private static Date localDateTimeToDate(LocalDateTime localDateTime)
	{
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static long daysBetweenTwoDates(Date dateFrom, Date dateTo)
	{
		return ChronoUnit.DAYS.between(Instant.ofEpochMilli(dateFrom.getTime()),
				Instant.ofEpochMilli(dateTo.getTime()));
	}
}