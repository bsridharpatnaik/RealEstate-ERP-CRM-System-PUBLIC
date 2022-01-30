package com.ec.application.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.AllInventoryAndInwardOutwardListProjection;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import javax.transaction.Transactional;

@Service
public class AsyncServiceInventory {

    @PersistenceContext
    private EntityManager entityManager;

    Logger log = LoggerFactory.getLogger(AsyncServiceInventory.class);

    @Transactional
    public void backFillClosingStock(String dbName, String id_list, Date date) throws Exception {
        try {
            ThreadLocalStorage.setTenantName(dbName);
            log.info("Starting backfilling closing stock");

            // Intentionally added sleep so that database data is saved before triggering Backfilling
            Thread.sleep((20000));

            StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("update_closing_stock");
            storedProcedure.registerStoredProcedureParameter("id_list", String.class, ParameterMode.IN);
            storedProcedure.setParameter("id_list", id_list );
            log.info("Before Procedure Start -" + LocalDateTime.now().toString());
            storedProcedure.execute();
            log.info("AFter Procedure End -" + LocalDateTime.now().toString());
            ThreadLocalStorage.setTenantName(null);
            log.info("Completed backfilling closing stock");
        } catch (Exception e) {
            throw new Exception("something went wrong!");

        }
    }

}
