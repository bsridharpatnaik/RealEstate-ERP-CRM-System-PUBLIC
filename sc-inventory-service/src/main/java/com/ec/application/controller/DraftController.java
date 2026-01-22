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

    @PostMapping
    public ResponseEntity<?> saveDraft(@RequestParam(name = "draftType") String draftType, @RequestBody String payload) throws Exception {
        if (!ALLOWED_DRAFT_TYPES.contains(draftType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only INDENT or PO draft types are allowed"));
        }
        draftService.saveOrUpdateDraftForUser(draftType.toUpperCase(), payload);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully saved draft");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getDraftPayload(@RequestParam(name = "draftType") String draftType) throws Exception {
        Draft draft = draftService.getDraftsByTypeForUser(draftType.toUpperCase());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(draft.getPayload());
    }
}