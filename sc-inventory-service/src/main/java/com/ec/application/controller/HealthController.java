package com.ec.application.controller;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
public class HealthController {

    @PersistenceContext
    private EntityManager em;

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
