package com.ec.application.controller;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.indentpo.IndentStatusBackfillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@UseDefaultTenant
public class AdminController {

    @Autowired
    IndentStatusBackfillService indentStatusBackfillService;

    @PostMapping("/indent-status-backfill/start")
    public ResponseEntity<Map<String, Object>> startIndentStatusBackfill() {
        return ResponseEntity.ok(indentStatusBackfillService.start());
    }

    @GetMapping("/indent-status-backfill/status")
    public ResponseEntity<Map<String, Object>> indentStatusBackfillStatus() {
        return ResponseEntity.ok(indentStatusBackfillService.getStatus());
    }
}
