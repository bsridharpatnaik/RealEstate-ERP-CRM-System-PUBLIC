package com.ec.application.config;

import com.ec.application.model.IndentSequence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class SequenceInitializer {

    @PersistenceContext
    private EntityManager em;

    private final SchemaConfig schemaConfig;

    public SequenceInitializer(SchemaConfig schemaConfig) {
        this.schemaConfig = schemaConfig;
    }

    @PostConstruct
    @Transactional
    public void initSequences() {
        schemaConfig.getSchemaMap().forEach((schemaName, code) -> {
            IndentSequence seq = em.find(IndentSequence.class, code);
            if (seq == null) {
                seq = new IndentSequence();
                seq.setTenantCode(code);
                seq.setLastId(0L);
                em.persist(seq);
            }
        });
    }
}
