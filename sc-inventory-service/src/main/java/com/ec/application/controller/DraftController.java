package com.ec.application.controller;

import com.ec.application.model.Draft;
import com.ec.application.repository.DraftRepository;
import com.ec.application.service.DraftService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @PostMapping()
    public ResponseEntity<?> saveDraft(
            @RequestParam(name = "draftType") String draftType,
            @RequestParam(name = "draftId", required = false) Long draftId,
            @RequestBody String payload) {
        if (!ALLOWED_DRAFT_TYPES.contains(draftType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only INDENT or PO draft types are allowed"));
        }
        draftId = draftService.saveOrUpdateDraft(draftId, draftType.toUpperCase(), payload);
        Map<String, Object> response = new HashMap<>();
        response.put("draftId", draftId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<?> getDraftPayload(@PathVariable Long draftId) {
        return draftService.getDraft(draftId)
                .map(draft -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(draft.getPayload()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<?> deleteDraft(@PathVariable Long draftId) {
        draftService.deleteDraft(draftId);
        return ResponseEntity.ok().body("Draft Deleted Successfully");
    }

    @GetMapping("list")
    public ResponseEntity<?> getDraftsByType(@RequestParam(name = "draftType") String draftType, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) {

        if (!ALLOWED_DRAFT_TYPES.contains(draftType.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body("Only INDENT or PO draft types are allowed");
        }
        Page<Draft> drafts = draftService.getDraftsByType(draftType.toUpperCase(), pageable);
        return ResponseEntity.ok(drafts);
    }
}