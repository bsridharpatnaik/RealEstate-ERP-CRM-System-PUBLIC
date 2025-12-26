package com.ec.application.config;

import com.ec.application.model.IndentSequence;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentSequenceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class SequenceInitializer implements ApplicationRunner {

    private final SchemaConfig schemaConfig;
    private final IndentSequenceRepository seqRepo;

    public SequenceInitializer(SchemaConfig schemaConfig, IndentSequenceRepository seqRepo) {
        this.schemaConfig = schemaConfig;
        this.seqRepo = seqRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        // initialize sequences for all tenants
        schemaConfig.getSchemaMap().forEach((schemaName, code) -> {
            try {
                // set tenant for routing datasource
                ThreadLocalStorage.setTenantName(schemaName);

                if (!seqRepo.existsById(code)) {
                    IndentSequence seq = new IndentSequence();
                    seq.setTenantCode(code);
                    seq.setLastId(0L);
                    seqRepo.save(seq);
                }
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        });
    }
}