package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.DraftSummary;
import com.ec.application.model.Draft;
import com.ec.application.repository.DraftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@UseDefaultTenant
public class DraftService {

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    TenantService tenantService;

    public void saveOrUpdateDraftForUser(String draftType, String payload) throws Exception {
        validateJson(payload);

        String username = userDetailsService.getCurrentUser().getUsername();

        final String tenant =
                "PO".equalsIgnoreCase(draftType)
                        ? null
                        : tenantService.fetchTenantFromHeader();

        Optional<Draft> existingDraft =
                tenant != null
                        ? draftRepository.findFirstByDraftTypeAndUsernameAndTenant(
                        draftType, username, tenant)
                        : draftRepository.findFirstByDraftTypeAndUsernameAndTenantIsNull(
                        draftType, username);

        Draft draft = existingDraft
                .map(d -> {
                    d.setPayload(payload);
                    return d;
                })
                .orElseGet(() -> {
                    Draft d = new Draft(draftType, payload, username);
                    d.setTenant(tenant);
                    return d;
                });

        draftRepository.save(draft);
    }

    /**
     * Creates a new named PO draft (always inserts — never upserts).
     * Draft name must be unique per user.
     */
    public void createNamedDraft(String draftType, String draftName, String payload) throws Exception {
        validateJson(payload);
        if (draftName == null || draftName.trim().isEmpty()) {
            throw new IllegalArgumentException("Draft name is required");
        }
        String trimmedName = draftName.trim();
        String username = userDetailsService.getCurrentUser().getUsername();

        if (draftRepository.existsByDraftTypeAndUsernameAndDraftNameAndTenantIsNull(draftType, username, trimmedName)) {
            throw new IllegalArgumentException("A draft with the name \"" + trimmedName + "\" already exists. Please choose a different name.");
        }

        Draft d = new Draft(draftType, payload, username);
        d.setDraftName(trimmedName);
        d.setTenant(null); // PO drafts are tenant-less
        draftRepository.save(d);
    }

    /**
     * Lists all PO drafts for the current user, newest first.
     */
    public List<DraftSummary> listDraftsForUser(String draftType) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();
        List<Draft> drafts = draftRepository.findAllByDraftTypeAndUsernameAndTenantIsNullOrderByCreationDateDesc(draftType, username);
        return drafts.stream()
                .map(d -> new DraftSummary(d.getDraftId(), d.getDraftName(), d.getCreationDate()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the payload for a specific draft, verifying it belongs to the current user.
     */
    public String getDraftPayloadById(Long draftId) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();
        Draft draft = draftRepository.findByDraftIdNotDeleted(draftId)
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        if (!username.equalsIgnoreCase(draft.getUsername())) {
            throw new RuntimeException("Access denied");
        }
        return draft.getPayload();
    }

    /**
     * Soft-deletes a specific draft, verifying it belongs to the current user.
     * No @Transactional here — transaction is opened at repository level so it starts
     * AFTER TenantAspect (@Before) has already set the schema to masterschema.
     */
    public void deleteDraftById(Long draftId) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();
        int updated = draftRepository.softDeleteByIdAndUsername(draftId, username);
        if (updated == 0) {
            throw new RuntimeException("Draft not found or access denied");
        }
    }

    public Draft getDraftsByTypeForUser(String draftType) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();

        final String tenant =
                "PO".equalsIgnoreCase(draftType)
                        ? null
                        : tenantService.fetchTenantFromHeader();

        return (tenant != null
                ? draftRepository.findFirstByDraftTypeAndUsernameAndTenant(
                draftType, username, tenant)
                : draftRepository.findFirstByDraftTypeAndUsernameAndTenantIsNull(
                draftType, username)
        ).orElseThrow(() ->
                new RuntimeException(
                        "No draft found for type: " + draftType +
                                ", user: " + username +
                                ", tenant: " + tenant
                )
        );
    }

    private void validateJson(String payload) {
        try {
            objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }
    }

    @UseDefaultTenant
    @Transactional
    public void deleteDraftForUser(String draftType) throws Exception {
        String username = userDetailsService.getCurrentUser().getUsername();

        // Resolve tenant ONCE
        final String tenant =
                "PO".equalsIgnoreCase(draftType)
                        ? null
                        : tenantService.fetchTenantFromHeader();

        Optional<Draft> draftOpt =
                tenant != null
                        ? draftRepository.findFirstByDraftTypeAndUsernameAndTenant(
                        draftType, username, tenant)
                        : draftRepository.findFirstByDraftTypeAndUsernameAndTenantIsNull(
                        draftType, username);

        // ✅ Idempotent soft delete
        draftOpt.ifPresent(draft ->
                draftRepository.softDeleteById(draft.getDraftId())
        );
    }
}