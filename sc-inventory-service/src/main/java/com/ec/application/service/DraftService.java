package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.Draft;
import com.ec.application.repository.DraftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
                    d.setTenant(tenant);   // null for PO, value for Indent
                    return d;
                });

        draftRepository.save(draft);
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