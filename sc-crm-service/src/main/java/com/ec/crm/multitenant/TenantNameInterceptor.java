package com.ec.crm.multitenant;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
public class TenantNameInterceptor extends HandlerInterceptorAdapter {
	
	@Value("${schemas.list}")
	private String schemasList;
	private Gson gson = new Gson();
	
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantName = request.getHeader("tenant-id");
        if(StringUtils.isBlank(schemasList)) {
        	response.setContentType("application/json");
        	response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(new Error("Tenants not initalized...")));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        
        if(!schemasList.contains(tenantName)) {
        	response.setContentType("application/json");
        	response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(new Error("User not allowed to access data")));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
    }
    
    @Setter
    @Getter
    @AllArgsConstructor
    public static class Error {
    	private String message;
    }
}
