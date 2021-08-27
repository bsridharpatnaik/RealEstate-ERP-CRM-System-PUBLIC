package com.ec.application.ReusableClasses;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CommonUtils {
    public String normalizePhoneNumber(String number) {
        number = number.replaceAll("[^+0-9]", ""); // All weird characters such as /, -, ...
        number = removeZero(number);
        return number;
    }

    public String removeZero(String str) {
        int i = 0;
        while (str.charAt(i) == '0')
            i++;
        StringBuffer sb = new StringBuffer(str);
        sb.replace(0, i, "");
        return sb.toString();  // return in String 
    }

    public static <T> String convertObjectToJson(T object) {
        ObjectMapper mapper = new ObjectMapper();
        try {

            String jsonString = mapper.writeValueAsString(object);
            return jsonString;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
