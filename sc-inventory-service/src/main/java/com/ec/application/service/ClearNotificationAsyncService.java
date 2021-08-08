package com.ec.application.service;

import com.ec.application.model.InventoryNotification;
import com.ec.application.repository.InventoryNotificationRepo;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ClearNotificationAsyncService {

    @Autowired
    InventoryNotificationRepo inventoryNotificationRepo;

    Logger log = LoggerFactory.getLogger(ContactService.class);

    public void clearNotifications(){
        Date dateBefore30Days = DateUtils.addDays(new Date(),-30);
        List<InventoryNotification> notifications = inventoryNotificationRepo.getExpiredNotifications(dateBefore30Days);
        log.info("Clearing Notifications.");
        log.info("No of notifications to be cleared - "+notifications.size());
        for(InventoryNotification in : notifications){
            inventoryNotificationRepo.softDelete(in);
        }
    }
}
