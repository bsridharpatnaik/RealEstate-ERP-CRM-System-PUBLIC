package com.ec.application.IDGenerator;

import com.ec.application.config.SpringContextHolder;
import com.ec.application.model.PurchaseOrderSequence;
import com.ec.application.repository.PurchaseOrderSequenceRepository;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class GlobalPurchaseOrderIdGenerator implements IdentifierGenerator {

    private static final String PREFIX = "PO";

    private PurchaseOrderSequenceRepository purchaseOrderSequenceRepository;

    private PurchaseOrderSequenceRepository getRepository() {
        if (purchaseOrderSequenceRepository == null) {
            purchaseOrderSequenceRepository =
                    SpringContextHolder.getBean(PurchaseOrderSequenceRepository.class);
        }
        return purchaseOrderSequenceRepository;
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
            throw new HibernateException("Failed to generate Purchase Order ID", e);
        }
    }
}
