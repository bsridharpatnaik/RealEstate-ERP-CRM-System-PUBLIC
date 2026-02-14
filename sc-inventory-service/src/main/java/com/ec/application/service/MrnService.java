package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.MrnSequence;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.MrnSequenceRepository;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
public class MrnService {

    private final MrnSequenceRepository repository;
    private final SchemaConfig schemaConfig;
    private final EntityManager entityManager;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public MrnService(MrnSequenceRepository repository, SchemaConfig schemaConfig, EntityManager entityManager, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.schemaConfig = schemaConfig;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long getNextMrn() {

        String master = schemaConfig.getMasterSchema();
        jdbcTemplate.update("UPDATE " + master + ".mrn_sequence_inward SET sequence_value = LAST_INSERT_ID(sequence_value + 1) WHERE id = 1");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}