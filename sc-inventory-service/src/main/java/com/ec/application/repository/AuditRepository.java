package com.ec.application.repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository 
{
	@Autowired
    private EntityManagerFactory emf;
	
	public EntityManager getSession()
	{
		return emf.unwrap(SessionFactory.class).createEntityManager();
	}

}
