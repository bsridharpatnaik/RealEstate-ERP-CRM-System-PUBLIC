package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentSequence;
import com.ec.application.repository.IndentSequenceRepository;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class SchemaPrefixedIdGenerator implements IdentifierGenerator {

    private IndentSequenceRepository indentSequenceRepository;

    private IndentSequenceRepository getRepository() {
        if (indentSequenceRepository == null) {
            indentSequenceRepository =
                    SpringContextHolder.getBean(IndentSequenceRepository.class);
        }
        return indentSequenceRepository;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object)
            throws HibernateException {

        if (!(object instanceof IndentInventory)) {
            throw new IllegalArgumentException("Unexpected object: " + object);
        }

        try {
            IndentInventory indent = (IndentInventory) object;
            String schemaCode = indent.getTenantSchemaCode();

            IndentSequenceRepository repo = getRepository();

            IndentSequence seq = repo.findById(schemaCode)
                    .orElseThrow(() ->
                            new RuntimeException("IndentSequence not found for tenantCode: " + schemaCode));

            seq.setLastId(seq.getLastId() + 1);
            repo.save(seq);

            return schemaCode + "-" + seq.getLastId();

        } catch (Exception e) {
            throw new HibernateException("Failed to generate ID for IndentInventory", e);
        }
    }
}
