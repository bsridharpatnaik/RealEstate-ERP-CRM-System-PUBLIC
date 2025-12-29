package com.ec.application.service;

import com.ec.application.model.Draft;
import com.ec.application.repository.DraftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import javax.persistence.Lob;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class DraftService {

    private final DraftRepository draftRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public DraftService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    public Long saveOrUpdateDraft(Long draftId, String draftType, String payload) {
        validateJson(payload);
        Draft draft;
        if (draftId != null) {
            draft = draftRepository.findById(draftId)
                    .orElseThrow(() ->
                            new RuntimeException("Draft with id " + draftId + " not found"));
            draft.setPayload(payload);
        } else {
            draft = new Draft(draftType, payload);
        }
        draftRepository.save(draft);
        return draft.getDraftId();
    }

    public Optional<Draft> getDraft(Long draftId) {
        return draftRepository.findById(draftId);
    }

    public Page<Draft> getDraftsByType(String draftType, Pageable pageable) {
        return draftRepository.findByDraftType(draftType, pageable);
    }

    public void deleteDraft(Long draftId) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() ->
                        new RuntimeException("Draft with id " + draftId + " not found"));
        draftRepository.delete(draft);
    }

    private void validateJson(String payload) {
        try {
            objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }
    }
}