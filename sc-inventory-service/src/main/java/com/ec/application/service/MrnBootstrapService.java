package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.MrnSequence;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.MrnSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
public class MrnBootstrapService {

    @Autowired
    private MrnSequenceRepository mrnSequenceRepository;

    @Autowired
    SchemaConfig schemaConfig;

    @PostConstruct
    @Transactional
    public void initializeMrnSequence() {

        // Switch to master schema
        ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());

        try {
            long count = mrnSequenceRepository.count();

            if (count == 0) {
                MrnSequence seq = new MrnSequence();
                seq.setId(1);
                seq.setSequenceValue(0L);
                mrnSequenceRepository.save(seq);
            }

        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
