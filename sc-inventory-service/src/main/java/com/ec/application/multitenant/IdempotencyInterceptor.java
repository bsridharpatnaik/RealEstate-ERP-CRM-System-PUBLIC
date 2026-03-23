package com.ec.application.multitenant;

import com.ec.application.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    @Autowired
    private IdempotencyService idempotencyService;

    private static final List<String> EXCLUDED_URL_FRAGMENTS =
            Arrays.asList("/list", "/search", "/fetch", "/export", "/download");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        boolean isExcluded = EXCLUDED_URL_FRAGMENTS.stream().anyMatch(uri::contains);
        if (isExcluded) return true;

        String key = request.getHeader("X-Idempotency-Key");
        if (key == null || key.isBlank()) return true;

        if (idempotencyService.isDuplicate(key)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Duplicate request ignored\"}");
            return false;
        }

        idempotencyService.markProcessed(key);
        return true;
    }
}
