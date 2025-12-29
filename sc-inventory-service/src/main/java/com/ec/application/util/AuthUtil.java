package com.ec.application.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public final class AuthUtil {

    private AuthUtil() {}

    public static String getAuthKey() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            return "SYSTEM";
        }

        HttpServletRequest request = attrs.getRequest();
        String auth = request.getHeader("Authorization");

        if (auth == null) {
            return "SYSTEM";
        }

        return Integer.toHexString(auth.hashCode());
    }
}
