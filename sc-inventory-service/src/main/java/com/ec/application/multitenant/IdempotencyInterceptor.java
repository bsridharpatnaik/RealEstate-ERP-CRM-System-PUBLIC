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

    /**
     * Exhaustive list of POST endpoints that create new records.
     * Only these URLs are subject to idempotency checking.
     * Add new create endpoints here as the application grows.
     */
    private static final List<String> CREATE_URLS = Arrays.asList(
            // Indent
            "/indent/create",

            // Purchase Order
            "/purchase-order/create",

            // Inward
            "/inward/create",
            "/inward/create/from-po",
            "/inward/opening-stock",

            // Outward
            "/outward/create",

            // Inventory Transfer
            "/inventory-transfer/create",

            // Lost / Damaged
            "/lostdamaged/create",

            // Machinery on Rent
            "/mor/create",

            // Product
            "/product/create",

            // Category
            "/category/create",

            // Location
            "/location/create",

            // Warehouse
            "/warehouse/create",

            // Supplier  (add if a create endpoint exists)
            // "/supplier/create",

            // Contact
            "/contact/create",

            // Firm
            "/firm/create",

            // Building Type
            "/buildingtype/create",

            // Usage Area
            "/usagearea/create",

            // Machinery (master)
            "/machinery/create",

            // Inventory Month Pricing
            "/inventorypricing/create",

            // BOQ
            "/boq/create",

            // File upload (creates a file record)
            "/file/upload"

            // NOTE: /drafts (POST) and /boqupload/boq_upload are intentionally excluded —
            // drafts are overwritten by design and BOQ upload is a bulk import, not a single create.
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        boolean isCreateUrl = CREATE_URLS.stream().anyMatch(uri::endsWith);
        if (!isCreateUrl) return true;

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
