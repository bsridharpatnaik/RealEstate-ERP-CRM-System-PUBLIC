package com.ec.common.Configuration;

import com.ec.common.Repository.ApiLogRepository;
import com.ec.common.Service.ApiLogService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String url = request.getRequestURI();
        String method = request.getMethod();
        String payload = getRequestBody(request);
        String username = getUsername(request);
        apiLogService.logToDatabase(url, method, payload, username);
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
}
