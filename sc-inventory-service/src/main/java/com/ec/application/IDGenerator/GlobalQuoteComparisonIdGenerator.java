package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.QuoteComparisonSequence;
import com.ec.application.repository.QuoteComparisonSequenceRepository;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class GlobalQuoteComparisonIdGenerator implements IdentifierGenerator {

    private static final String PREFIX = "QC";

    private QuoteComparisonSequenceRepository repository;

    private QuoteComparisonSequenceRepository getRepository() {
        if (repository == null) {
            repository = SpringContextHolder.getBean(QuoteComparisonSequenceRepository.class);
        }
        return repository;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object)
            throws HibernateException {
        try {
            QuoteComparisonSequenceRepository repo = getRepository();

            QuoteComparisonSequence seq = repo.findForUpdate(PREFIX)
                    .orElseGet(() -> {
                        QuoteComparisonSequence s = new QuoteComparisonSequence();
                        s.setPrefix(PREFIX);
                        s.setLastId(0L);
                        return repo.save(s);
                    });

            Long nextId = seq.getLastId() + 1;
            seq.setLastId(nextId);
            repo.save(seq);

            return PREFIX + "-" + nextId;

        } catch (Exception e) {
            throw new HibernateException("Failed to generate Quote Comparison ID", e);
        }
    }
}
