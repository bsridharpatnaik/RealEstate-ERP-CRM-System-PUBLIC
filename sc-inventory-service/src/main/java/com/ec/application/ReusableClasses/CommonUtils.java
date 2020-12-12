package com.ec.application.ReusableClasses;

public class CommonUtils 
{
	public String normalizePhoneNumber(String number) 
    {
        number = number.replaceAll("[^+0-9]", ""); // All weird characters such as /, -, ...
        number = removeZero(number);
        return number;
    }

	public String removeZero(String str) 
    { 
        int i = 0; 
        while (str.charAt(i) == '0') 
            i++; 
        StringBuffer sb = new StringBuffer(str); 
        sb.replace(0, i, ""); 
        return sb.toString();  // return in String 
    }
}
