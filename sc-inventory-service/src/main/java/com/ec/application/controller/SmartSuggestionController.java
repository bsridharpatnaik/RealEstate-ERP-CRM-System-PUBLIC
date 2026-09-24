package com.ec.application.controller;

import com.ec.application.aspects.AllowOnly;
import com.ec.application.constants.RoleConstants;
import com.ec.application.service.SmartSuggestionService;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** "✨ Smart suggestion" panels (rule-based, no AI). Reads master tables: listed in TenantNameInterceptor's excluded patterns. */
@RestController
@RequestMapping("/smart-suggestion")
public class SmartSuggestionController {

    @Autowired
    private SmartSuggestionService smartSuggestionService;

    @Autowired
    private UserDetailsService userDetailsService;

    /** Indent details, shown next to Approve — same roles as the approve endpoint. */
    @GetMapping("/indent/{indentId}")
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER, RoleConstants.PROJECT_MANAGER})
    public Map<String, Object> indent(@PathVariable String indentId) throws Exception {
        return smartSuggestionService.indentCheck(indentId);
    }

    /** Create PO: "also on order" per item, across projects. Same roles as previous rates. */
    @GetMapping("/open-pos")
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER, RoleConstants.MANAGEMENT})
    public List<Map<String, Object>> openPos(@RequestParam List<Long> productIds,
                                             @RequestParam(required = false) String excludePoId) {
        return smartSuggestionService.openPurchaseOrders(productIds, excludePoId);
    }

    /** Global Dashboard "Needs attention" banner — lines depend on the user's role and projects. */
    @GetMapping("/attention")
    public List<Map<String, Object>> attention() throws Exception {
        boolean buyer = userDetailsService.hasRole(RoleConstants.ADMIN) || userDetailsService.hasRole(RoleConstants.PURCHASE_MANAGER);
        boolean approver = buyer || userDetailsService.hasRole(RoleConstants.PROJECT_MANAGER);
        return smartSuggestionService.attention(userDetailsService.getCurrentUserAllowedSchemas(), approver, buyer);
    }

    /** PO details — anyone who can open the PO. */
    @GetMapping("/po/{poId}")
    public Map<String, Object> po(@PathVariable String poId) throws Exception {
        // Same rule as PurchaseOrderPriceMasker: project managers / store in-charges never see money.
        return smartSuggestionService.poSummary(poId, !userDetailsService.isPriceRestricted());
    }
}
