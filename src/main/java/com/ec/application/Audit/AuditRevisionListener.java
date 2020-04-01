package com.ec.application.Audit;



import org.hibernate.envers.RevisionListener;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity audit = (AuditRevisionEntity) revisionEntity;
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        audit.setUsername("ss");
    }
}
