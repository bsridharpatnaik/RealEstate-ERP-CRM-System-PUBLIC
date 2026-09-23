package com.ec.common.Configuration;

import com.ec.common.Repository.ApiLogRepository;
import com.ec.common.Service.ApiLogService;
import com.ec.common.Service.RecentActivityService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class LoggingZuulFilter extends ZuulFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingZuulFilter.class);

    @Autowired
    private UsernameExtractor usernameExtractor;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    ApiLogRepository apiLogRepository;

    @Autowired
    ApiLogService apiLogService;

    @Autowired
    RecentActivityService recentActivityService;

    // Full request logging to DB (heavy: reads body + JPA insert per request). Off by default.
    @Value("${apilog.enabled:false}")
    private boolean apiLogEnabled;

    @Override
    public String filterType() {
        return "pre"; // or "post" for response logging
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        // Run only if something needs it; when both are off this filter does nothing.
        return apiLogEnabled || recentActivityService.isEnabled();
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String url = request.getRequestURI();
        String method = request.getMethod();
        String username = getUsername(request);
        String tenantName = getTenantName(request);

        // Cheap in-memory activity record (powers the live-activity view).
        recentActivityService.record(username, method, url, tenantName);

        // Heavy path: only when explicitly enabled (reads request body + DB insert).
        if (apiLogEnabled) {
            String payload = getRequestBody(request);
            apiLogService.logToDatabase(tenantName, url, method, payload, username);
        }
        return null;
    }

    private String getRequestBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            logger.error("Error reading request body", e);
            return "";
        }
    }
    private String getUsername(HttpServletRequest request) {
        if ("/ec/login".equals(request.getRequestURI())) {
            return usernameExtractor.extractUsernameFromLoginPayload(request);
        } else {
            return securityUtils.getUsername();
        }
    }

    private String getTenantName(HttpServletRequest request) {
        String tenantName = request.getHeader("Tenant-Id");
        return tenantName==null?"":tenantName;
    }
}
