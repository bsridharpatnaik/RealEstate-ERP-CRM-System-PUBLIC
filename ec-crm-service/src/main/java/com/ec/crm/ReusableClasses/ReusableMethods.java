package com.ec.crm.ReusableClasses;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+ 
                "[a-zA-Z0-9_+&*-]+)*@" + 
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" + 
                "A-Z]{2,7}$"; 
                  
		Pattern pat = Pattern.compile(emailRegex); 
		if (email == null) 
			return false; 
		return pat.matcher(email).matches(); 
	} 
}