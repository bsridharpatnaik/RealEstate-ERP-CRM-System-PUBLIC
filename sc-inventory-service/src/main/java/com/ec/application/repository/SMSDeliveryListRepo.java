package com.ec.application.repository;

import com.ec.application.model.SMSDeliveryList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SMSDeliveryListRepo extends JpaRepository<SMSDeliveryList,String> {
}
