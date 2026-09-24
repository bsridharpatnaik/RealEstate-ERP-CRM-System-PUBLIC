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
                    Pattern.compile(".*/firm.*"),
                    Pattern.compile(".*/error.*"),
                    Pattern.compile(".*/master-file.*"),      // master-schema file upload/download (PO attachments)
                    Pattern.compile(".*/admin.*"),           // admin/backfill endpoints — always master schema
                    Pattern.compile(".*/quote-comparison.*"), // quote comparison — master schema global module
                    Pattern.compile(".*/health.*"),          // public DB health check — no tenant header, routes to master
                    Pattern.compile(".*/ai-assistant.*"),    // admin AI chat — global, not project-scoped
                    Pattern.compile(".*/smart-suggestion.*"), // indent/PO smart suggestions — read master tables
                    // Global reports — master-schema sync tables, routed via @UseDefaultTenant and
                    // filtered per-query by getCurrentUserAllowedSchemas(). They must NOT be gated on the
                    // caller's selected tenant-id (a store-incharge may not have access to the currently
                    // selected project yet still needs the cross-tenant report of their own tenants).
                    // NB: /indent-fulfillment is already covered by the .*/indent.* pattern above.
                    Pattern.compile(".*/fifo-report.*"),
                    Pattern.compile(".*/stock-aging.*"),
                    Pattern.compile(".*/expired-stock.*"),
                    // NB: match only the global report path (/dead-stock-report), NOT /dead-stock —
                    // the latter (DeadStockController) has tenant-scoped endpoints used in the indent flow
                    // (GET /dead-stock?productId=) which must keep the tenant header.
                    Pattern.compile(".*/dead-stock-report.*"),
                    Pattern.compile(".*/po-recon.*"),
                    Pattern.compile(".*/low-stock.*"),
                    // Stock Summary: service is @UseDefaultTenant (master) and its import uses the per-row
                    // tenantSchema from the file, so no endpoint here depends on the caller's tenant header.
                    // Data is scoped to the user's allowed tenants inside StockSummaryCustomRepoImpl.
                    Pattern.compile(".*/stock-summary.*")
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

        boolean isGlobalBOQRequest = "true".equals(request.getHeader("X-Global-BOQ"));
        boolean isProjectManager = currentUser.getRoles() != null &&
                currentUser.getRoles().contains("project-manager");

        if (!currentUser.getAllowedTenants().contains(tenantName)) {
            if (isGlobalBOQRequest && isProjectManager) {
                // project-managers may read any tenant's BOQ data via the Global BOQ View
            } else {
                writeError(response, "User not allowed to access data for this project - " + tenantName);
                return false;
            }
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

