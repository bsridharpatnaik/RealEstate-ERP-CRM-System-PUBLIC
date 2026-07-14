package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.PurchaseOrderSequence;
import com.ec.application.repository.PurchaseOrderSequenceRepository;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * Reuses the generic purchase_order_sequence table (keyed by an arbitrary prefix string,
 * not actually PO-specific) with its own "SO" prefix — avoids a redundant sequence table.
 */
public class GlobalServiceOrderIdGenerator implements IdentifierGenerator {

    private static final String PREFIX = "SO";

    private PurchaseOrderSequenceRepository sequenceRepository;

    private PurchaseOrderSequenceRepository getRepository() {
        if (sequenceRepository == null) {
            sequenceRepository = SpringContextHolder.getBean(PurchaseOrderSequenceRepository.class);
        }
        return sequenceRepository;
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object)
            throws HibernateException {

        try {
            PurchaseOrderSequenceRepository repo = getRepository();

            PurchaseOrderSequence seq = repo.findForUpdate(PREFIX)
                    .orElseGet(() -> {
                        PurchaseOrderSequence s = new PurchaseOrderSequence();
                        s.setTenantCode(PREFIX);
                        s.setLastId(0L);
                        return repo.save(s);
                    });

            Long nextId = seq.getLastId() + 1;
            seq.setLastId(nextId);
            repo.save(seq);

            return PREFIX + "-" + nextId;

        } catch (Exception e) {
            throw new HibernateException("Failed to generate Service Order ID", e);
        }
    }
}
