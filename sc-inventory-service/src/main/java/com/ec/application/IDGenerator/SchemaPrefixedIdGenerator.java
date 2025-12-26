package com.ec.application.IDGenerator;

import com.ec.application.model.Indent;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;

public class SchemaPrefixedIdGenerator implements IdentifierGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        if (!(object instanceof Indent)) {
            throw new IllegalArgumentException("Unexpected object: " + object);
        }

        Indent indent = (Indent) object;
        String schemaCode = indent.getTenantSchemaCode(); // set before persist

        // increment last_id in DB and fetch updated value atomically
        entityManager.getTransaction().begin();
        entityManager.createQuery("UPDATE IndentSequence s SET s.lastId = s.lastId + 1 WHERE s.tenantCode = :code")
                .setParameter("code", schemaCode)
                .executeUpdate();

        Long nextId = (Long) entityManager.createQuery("SELECT s.lastId FROM IndentSequence s WHERE s.tenantCode = :code")
                .setParameter("code", schemaCode)
                .getSingleResult();
        entityManager.getTransaction().commit();

        return schemaCode + String.format("%06d", nextId);
    }
}

