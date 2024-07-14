package com.ec.crm.aspects;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.Lead;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Service.UserDetailsService;
import com.ec.crm.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AccessInterceptor implements HandlerInterceptor {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private LeadRepo leadRepo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();

            if (method.isAnnotationPresent(CheckAccess.class)) {
                Object uriTemplateVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (uriTemplateVars instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> pathVariables = (Map<String, String>) uriTemplateVars;
                    Long id = pathVariables != null ? Long.valueOf(pathVariables.get("id")) : null;

                    if (id != null) {
                        // Extract tenant-id from the header
                        String tenantId = request.getHeader("tenant-id");
                        if (tenantId != null && !tenantId.isEmpty()) {
                            ThreadLocalStorage.setTenantName(tenantId);
                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant-id header");
                            return false;
                        }

                        UserReturnData currentUser = userDetailsService.getCurrentUser();
                        List<String> roles = currentUser.getRoles().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList());

                        if (!roles.contains("admin") && !roles.contains("manager")) {
                            Optional<Lead> leadOpt = leadRepo.findById(id);
                            if (!leadOpt.isPresent()) {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Lead with ID -" + id + " Not Found");
                                return false;
                            }

                            if (!currentUser.getId().equals(leadOpt.get().getAsigneeId())) {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User Not Allowed to access data!");
                                return false;
                            }
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid ID");
                        return false;
                    }
                }
            }
        }
        return true;
    }
}