package com.ec.application.multitenant;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ec.application.config.SchemaConfig;
import com.ec.application.data.UserReturnData;
import com.ec.application.service.TenantService;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Component
public class TenantNameInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private SchemaConfig schemaConfig;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${master.schema}")
    private String defaultTenant;

    @Autowired
    TenantService tenantService;

    @Autowired
    UserDetailsService userDetailsService;

    private static final Gson GSON = new Gson();

    private static final List<Pattern> EXCLUDED_URL_PATTERNS =
            Collections.unmodifiableList(Arrays.asList(
                    Pattern.compile(".*/category.*"),
                    Pattern.compile(".*/contact.*"),
                    Pattern.compile(".*/contractor.*"),
                    Pattern.compile(".*/supplier.*"),
                    Pattern.compile(".*/machinery.*"),
                    Pattern.compile(".*/product.*"),
                    Pattern.compile(".*/inventory-transfer.*"),
                    Pattern.compile(".*/indent.*"),
                    Pattern.compile(".*/purchase-order.*"),
                    Pattern.compile(".*/charts.*"),
                    Pattern.compile(".*/trend.*"),
                    Pattern.compile(".*/stale-charts.*"),
                    Pattern.compile(".*/error.*")
            ));


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (shouldSkipTenantCheck(request)) {
            ThreadLocalStorage.setTenantName(defaultTenant);
            return true;
        }

        String tenantName = request.getHeader("tenant-id");

        if (tenantName == null || tenantName.isEmpty()) {
            writeError(response, "Missing tenant-id header");
            return false;
        }

        UserReturnData currentUser = userDetailsService.getCurrentUser();

        // Validate tenants initialized
        if (schemaConfig.getSchemaMap() == null || schemaConfig.getSchemaMap().isEmpty()) {
            writeError(response, "Tenants not initialized...");
            return false;
        }

        // Validate tenant access
        if (!schemaConfig.getSchemaMap().containsKey(tenantName)) {
            writeError(response, "Invalid tenant-id header received in request. Please contact system administrator.");
            return false;
        }

        if (!currentUser.getAllowedTenants().contains(tenantName)) {
            writeError(response, "User not allowed to access data for this project - " + tenantName);
            return false;
        }

        ThreadLocalStorage.setTenantName(tenantName);
        return true;
    }

    /**
     * Check whether the current request should bypass tenant validation
     */
    private boolean shouldSkipTenantCheck(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return EXCLUDED_URL_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(uri).matches());
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        ThreadLocalStorage.setTenantName(null);
    }

    private void writeError(HttpServletResponse response, String message) throws IOException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(GSON.toJson(new Error(message)));
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class Error {
        private String message;
    }
}

