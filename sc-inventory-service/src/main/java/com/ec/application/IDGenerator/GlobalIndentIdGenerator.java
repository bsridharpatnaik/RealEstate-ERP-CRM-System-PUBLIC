package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.IndentSequence;
import com.ec.application.repository.IndentSequenceRepository;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class GlobalIndentIdGenerator implements IdentifierGenerator {

    private static final String PREFIX = "IN";

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

        try {
            IndentSequenceRepository repo = getRepository();

            IndentSequence seq = repo.findForUpdate(PREFIX)
                    .orElseGet(() -> {
                        IndentSequence s = new IndentSequence();
                        s.setTenantCode(PREFIX);
                        s.setLastId(0L);
                        return repo.save(s);
                    });

            Long nextId = seq.getLastId() + 1;
            seq.setLastId(nextId);
            repo.save(seq);

            return PREFIX + "-" + nextId;

        } catch (Exception e) {
            throw new HibernateException("Failed to generate Indent ID", e);
        }
    }
}
