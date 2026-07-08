package com.ec.application.controller;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
@Conditional(HealthController.ProdProfileCondition.class) // only registered when an active profile contains "prod"
public class HealthController {

    @PersistenceContext
    private EntityManager em;

    /**
     * Active only on production. Profile names here carry a suffix (e.g. sc-prod-v2, ec-prod),
     * so we substring-match "prod" rather than using @Profile("prod") (exact-name only).
     * On local/qa the bean isn't created and /health returns 404.
     */
    static class ProdProfileCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            for (String profile : context.getEnvironment().getActiveProfiles()) {
                if (profile != null && profile.toLowerCase().contains("prod")) return true;
            }
            return false;
        }
    }

    // Public: URL matches the ".*/health.*" excluded pattern in TenantNameInterceptor,
    // so no tenant-id header is required and it routes to master schema.
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        try {
            em.createNativeQuery("SELECT 1").getSingleResult(); // proves DB reachable
            body.put("status", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("status", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}
