package com.ec.crm.Repository;

import com.ec.crm.Model.PaymentReceived;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Repository
public interface PaymentReceivedRepo extends BaseRepository<PaymentReceived, Long> {

    @Query("SELECT CASE WHEN SUM(p.amount) IS NULL THEN 0 ELSE SUM(p.amount) END FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId AND p.paymentBy='Customer'")
    Double findTotalCustomerPaymentsByDealStructureId(@RequestParam("dealStructureId") Long dealStructureId);

    @Query("SELECT CASE WHEN SUM(p.amount) IS NULL THEN 0 ELSE SUM(p.amount) END  FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId AND p.paymentBy='Bank'")
    Double findTotalBankPaymentsByDealStructureId(@RequestParam("dealStructureId") Long dealStructureId);

    @Query("SELECT p FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId AND p.paymentBy='Customer' ORDER BY p.paymentReceivedDate")
    List<PaymentReceived> findCustomerPaymentsByDealStructureId(@RequestParam("dealStructureId") Long dealStructureId);

    @Query("SELECT p FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId AND p.paymentBy='Bank' ORDER BY p.paymentReceivedDate")
    List<PaymentReceived> findBankPaymentsByDealStructureId(@RequestParam("dealStructureId") Long dealStructureId);

    @Query("SELECT p FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId ORDER BY p.paymentReceivedDate")
    List<PaymentReceived> findAllPaymentsByDealStructureId(@RequestParam("dealStructureId") Long dealStructureId);

    @Query("SELECT SUM(amount) FROM PaymentReceived p WHERE p.ds.dealId=:dealStructureId")
    Double getTotalReceivedByDealStructure(@RequestParam("dealStructureId")Long dealStructureId);

    @Query("SELECT COUNT(p) FROM PaymentReceived p WHERE p.ds.lead.leadId=:id")
    int getPaymentsForLead(@RequestParam("id")Long id);
}
