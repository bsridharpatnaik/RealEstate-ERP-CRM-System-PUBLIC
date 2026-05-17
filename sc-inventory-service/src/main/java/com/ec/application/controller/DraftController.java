package com.ec.application.controller;

import com.ec.application.data.DraftSummary;
import com.ec.application.model.Draft;
import com.ec.application.repository.DraftRepository;
import com.ec.application.service.DraftService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/drafts")
public class DraftController {

    private static final List<String> ALLOWED_DRAFT_TYPES =
            Arrays.asList("INDENT", "PO");

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    // ── Legacy single-draft endpoints (used by Indent and auto-save fallback) ──

    @PostMapping
    public ResponseEntity<?> saveDraft(@RequestParam(name = "draftType") String draftType,
                                       @RequestParam(name = "draftName", required = false) String draftName,
                                       @RequestBody String payload) throws Exception {
        if (!ALLOWED_DRAFT_TYPES.contains(draftType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only INDENT or PO draft types are allowed"));
        }
        if (draftName != null && !draftName.trim().isEmpty()) {
            // Named PO draft — always create new
            draftService.createNamedDraft(draftType.toUpperCase(), draftName, payload);
        } else {
            // Legacy upsert (Indent drafts and auto-save fallback)
            draftService.saveOrUpdateDraftForUser(draftType.toUpperCase(), payload);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Draft saved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getDraftPayload(@RequestParam(name = "draftType") String draftType) throws Exception {
        Draft draft = draftService.getDraftsByTypeForUser(draftType.toUpperCase());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(draft.getPayload());
    }

    // ── Multi-draft endpoints (PO) ──

    @GetMapping("/list")
    public ResponseEntity<?> listDrafts(@RequestParam(name = "draftType") String draftType) throws Exception {
        if (!ALLOWED_DRAFT_TYPES.contains(draftType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only INDENT or PO draft types are allowed"));
        }
        List<DraftSummary> drafts = draftService.listDraftsForUser(draftType.toUpperCase());
        return ResponseEntity.ok(drafts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDraftById(@PathVariable Long id) throws Exception {
        String payload = draftService.getDraftPayloadById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDraftById(@PathVariable Long id) throws Exception {
        draftService.deleteDraftById(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Draft deleted"));
    }
}