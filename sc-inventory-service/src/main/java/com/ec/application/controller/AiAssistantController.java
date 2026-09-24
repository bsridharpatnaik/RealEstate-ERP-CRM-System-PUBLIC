package com.ec.application.controller;

import com.ec.application.aspects.AllowOnly;
import com.ec.application.constants.RoleConstants;
import com.ec.application.service.AiAssistantService;
import com.ec.application.service.AiUsageService;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** AI chat for admins + purchase managers, with per-user daily limits and usage history. Not project-scoped: listed in TenantNameInterceptor's excluded patterns. */
@RestController
@RequestMapping("/ai-assistant")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private AiUsageService aiUsageService;

    @Autowired
    private UserDetailsService userDetailsService;

    /** Body: {"question": "...", "sessionId": "<optional, to continue a conversation>"} */
    @PostMapping("/ask")
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public Map<String, Object> ask(@RequestBody Map<String, String> body) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();
        return aiAssistantService.ask(username, body.get("question"), body.get("sessionId"));
    }

    @GetMapping("/usage")
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public Map<String, Object> usage() throws Exception {
        return aiUsageService.usage(userDetailsService.getCurrentUser().getUsername());
    }

    /** Admin "AI Usage" page: per-user requests today / last 30 days and daily limits. */
    @GetMapping("/usage-report")
    @AllowOnly(roles = {RoleConstants.ADMIN})
    public Map<String, Object> usageReport() {
        return aiUsageService.report();
    }

    /** Admin "AI Usage" page: who asked what. username omitted = everyone. */
    @GetMapping("/history")
    @AllowOnly(roles = {RoleConstants.ADMIN})
    public List<Map<String, Object>> history(@RequestParam(required = false) String username,
                                             @RequestParam(defaultValue = "30") int days) {
        return aiUsageService.history(username, days);
    }

    /** Body: {"username": "<user or * for the default>", "dailyRequests": <n, or null to use the default>} */
    @PutMapping("/limits")
    @AllowOnly(roles = {RoleConstants.ADMIN})
    public Map<String, Object> setLimit(@RequestBody Map<String, Object> body) {
        Object n = body.get("dailyRequests");
        aiUsageService.setLimit((String) body.get("username"), n == null || "".equals(n) ? null : Integer.valueOf(n.toString()));
        return aiUsageService.report();
    }
}
