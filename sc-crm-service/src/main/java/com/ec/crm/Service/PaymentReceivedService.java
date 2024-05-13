package com.ec.crm.Service;

import com.ec.crm.Data.DealStructurePaymentReceivedDTO;
import com.ec.crm.Data.PaymentReceivedCreateData;
import com.ec.crm.Data.PaymentReceivedDTO;
import com.ec.crm.Data.ValidEnumsForPaymentReceived;
import com.ec.crm.Enums.PaymentReceivedFromEnum;
import com.ec.crm.Enums.PaymentReceivedPaymentModeEnum;
import com.ec.crm.Model.PaymentReceived;
import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.PaymentReceivedRepo;
import com.ec.crm.Repository.PaymentScheduleRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentReceivedService {

    @Autowired
    PaymentReceivedRepo paymentReceivedRepo;

    @Autowired
    DealStructureRepo dealStructureRepo;

    @Autowired
    PaymentScheduleRepo paymentScheduleRepo;

    @Autowired
    AllActivitiesService allActivitiesService;

    @Autowired
    PaymentScheduleService paymentScheduleService;

    @Transactional(rollbackFor = Exception.class)
    public PaymentReceived createNewPayment(PaymentReceivedCreateData payload) throws Exception {
        PaymentReceived paymentReceived = new PaymentReceived();
        validatePayload(payload);
        PaymentSchedule ps = exitIfScheduleDoesntExists(payload);
        setFields(paymentReceived, payload);
        paymentReceivedRepo.save(paymentReceived);
        paymentScheduleService.backFillScheduleStatus(ps, paymentReceived, "create");
        return paymentReceived;
    }

 /*   @Transactional(rollbackFor = Exception.class)
    public PaymentReceived updatePayment(PaymentReceivedCreateData payload) throws Exception {
        validatePayload(payload);
        PaymentReceived paymentReceived = paymentReceivedRepo.findById(payload.getPaymentId()).get();
        exitIfScheduleDoesntExists(payload);
        setFields(paymentReceived, payload);
        paymentReceivedRepo.save(paymentReceived);
        backFillScheduleStatus(paymentReceived.getDs().getDealId());
        return paymentReceived;
    }*/

    private PaymentSchedule exitIfScheduleDoesntExists(PaymentReceivedCreateData payload) throws Exception {
        List<PaymentSchedule> pSchedules = paymentScheduleRepo.getSchedulesForDeal(payload.getDealStructureId());
        for (PaymentSchedule ps : pSchedules) {
            if (!ps.getIsReceived() && ps.getAmount().equals(payload.getAmount())) {
                if (ps.getIsCustomerPayment() && payload.getPaymentBy().equals(PaymentReceivedFromEnum.Customer)) {
                    return ps;
                } else if (!ps.getIsCustomerPayment() && payload.getPaymentBy().equals(PaymentReceivedFromEnum.Bank)) {
                    return ps;
                }
            }
        }
        throw new Exception(("There is NO Payment scheduled for Amount - " + payload.getAmount()) + " From " + payload.getPaymentBy());
    }

    public DealStructurePaymentReceivedDTO getPaymentsList(Long dealStructureId) {
        List<PaymentReceivedDTO> customerPayments = paymentReceivedRepo.findCustomerPaymentsByDealStructureId(dealStructureId).stream().map(this::convertToDTO).collect(Collectors.toList());
        List<PaymentReceivedDTO> bankPayments = paymentReceivedRepo.findBankPaymentsByDealStructureId(dealStructureId).stream().map(this::convertToDTO).collect(Collectors.toList());
        return new DealStructurePaymentReceivedDTO(customerPayments, bankPayments);
    }

    private PaymentReceivedDTO convertToDTO(PaymentReceived paymentReceived) {
        return new PaymentReceivedDTO(paymentReceived);
    }

    private void setFields(PaymentReceived paymentReceived, PaymentReceivedCreateData payload) {
        paymentReceived.setPaymentBy(payload.getPaymentBy());
        paymentReceived.setPaymentReceivedDate(payload.getPaymentReceivedDate());
        paymentReceived.setAmount(payload.getAmount());
        paymentReceived.setBankName(payload.getBankName() == null ? null : payload.getBankName());
        paymentReceived.setReferenceNumber(payload.getReferenceNumber() == null ? null : payload.getReferenceNumber());
        paymentReceived.setPaymentMode(payload.getPaymentMode());
        paymentReceived.setDs(dealStructureRepo.findById(payload.getDealStructureId()).get());
    }

    private void validatePayload(PaymentReceivedCreateData payload) throws Exception {
        if (payload.getPaymentId() != null) {
            if (!paymentReceivedRepo.existsById(payload.getPaymentId())) {
                throw new Exception("Payment Received not found with ID " + payload.getPaymentId());
            }
        }
        List<String> missingFields = checkRequiredFields(payload);
        if (missingFields.size() > 0)
            throw new Exception("Below required fields are missing from the request - " + String.join(",", missingFields));

        if (payload.getAmount() <= 0)
            throw new Exception("Amount cannot be less than or equals to zero!");

        if (!dealStructureRepo.existsById(payload.getDealStructureId()))
            throw new Exception("Deal Structure not found with ID " + payload.getDealStructureId());
    }

    private List<String> checkRequiredFields(PaymentReceivedCreateData payload) {
        List<String> missingFields = new ArrayList<>();
        if (payload.getPaymentReceivedDate() == null)
            missingFields.add("Payment Received Date");
        if (payload.getPaymentBy() == null)
            missingFields.add("Payment By");
        if (payload.getPaymentMode() == null)
            missingFields.add("Payment Mode");
        if (payload.getAmount() == null)
            missingFields.add("Amount");
        if (payload.getDealStructureId() == null)
            missingFields.add("Deal Structure ID");
        return missingFields;
    }

    public ValidEnumsForPaymentReceived getValidEnumForDropdown() {
        ValidEnumsForPaymentReceived validEnumsForPaymentReceived = new ValidEnumsForPaymentReceived();
        validEnumsForPaymentReceived.setValidMPaymentMode(PaymentReceivedPaymentModeEnum.getValidPaymentReceivedTypeEnumValues());
        validEnumsForPaymentReceived.setValidPaymentFrom(PaymentReceivedFromEnum.getValidPaymentReceivedTypeEnumValues());
        return validEnumsForPaymentReceived;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePayment(Long id) throws Exception {
        Optional<PaymentReceived> paymentReceived = paymentReceivedRepo.findById(id);
        if (paymentReceived.isPresent()) {
            PaymentSchedule ps = paymentScheduleRepo.findByPaymentReceived(paymentReceived.get().getPaymentId());
            paymentScheduleService.backFillScheduleStatus(ps, paymentReceived.get(), "delete");
            paymentReceivedRepo.softDeleteById(id);
        } else
            throw new Exception("Payment Received with ID " + id + " not found!");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSinglePaymentWithoutBackfill(Long id) throws Exception {
        Optional<PaymentReceived> paymentReceived = paymentReceivedRepo.findById(id);
        if (paymentReceived.isPresent()) {
            paymentReceivedRepo.softDeleteById(id);
        } else
            throw new Exception("Payment Received with ID " + id + " not found!");
    }

    public Boolean getPaymentStepperStatus(Long id) {
        int count = paymentReceivedRepo.getPaymentsForLead(id);
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePaymentsForDeal(Long id) {
        List<PaymentReceived> allPaymentsForDeal = paymentReceivedRepo.findAllPaymentsByDealStructureId(id);
        for (PaymentReceived pr : allPaymentsForDeal) {
            paymentReceivedRepo.softDeleteById(pr.getPaymentId());
        }
    }

/*    @Transactional(rollbackFor = Exception.class)
    public void backFillScheduleStatus(Long dealId) {
        Double totalCustomerPayment = paymentReceivedRepo.findTotalCustomerPaymentsByDealStructureId(dealId);
        Double totalBankPayment = paymentReceivedRepo.findTotalBankPaymentsByDealStructureId(dealId);

        List<PaymentSchedule> schedules = paymentScheduleRepo.getSchedulesForDeal(dealId);

        schedules = schedules.stream().sorted((o1, o2) -> o1.getPaymentDate().compareTo(o2.getPaymentDate())).collect(Collectors.toList());

        double scheduleCustomerTotalAmount = (double) 0;
        double scheduleBankTotalAmount = (double) 0;

        for (PaymentSchedule schedule : schedules) {
            if (schedule.getIsCustomerPayment()) {
                scheduleCustomerTotalAmount = scheduleCustomerTotalAmount + schedule.getAmount();
                if (scheduleCustomerTotalAmount <= totalCustomerPayment) {
                    if (!schedule.getIsReceived()) {
                        schedule.setIsReceived(true);
                        if (schedule.getLa() != null) {
                            allActivitiesService.updateLeadActivityStatus(schedule.getLa().getLeadActivityId(), false);
                        }
                        paymentScheduleRepo.save(schedule);
                    } else {
                        if (schedule.getIsReceived()) {
                            schedule.setIsReceived(false);
                            if (schedule.getLa() != null) {
                                allActivitiesService.updateLeadActivityStatus(schedule.getLa().getLeadActivityId(), true);
                            }
                            paymentScheduleRepo.save(schedule);
                        }
                    }
                }
            } else if (!schedule.getIsCustomerPayment()) {
                scheduleBankTotalAmount = scheduleBankTotalAmount + schedule.getAmount();
                if (scheduleBankTotalAmount <= totalBankPayment) {
                    if (!schedule.getIsReceived()) {
                        schedule.setIsReceived(true);
                        if (schedule.getLa() != null) {
                            allActivitiesService.updateLeadActivityStatus(schedule.getLa().getLeadActivityId(), false);
                        }
                        paymentScheduleRepo.save(schedule);
                    }
                } else {
                    if (schedule.getIsReceived()) {
                        schedule.setIsReceived(false);
                        if (schedule.getLa() != null) {
                            allActivitiesService.updateLeadActivityStatus(schedule.getLa().getLeadActivityId(), false);
                        }
                        paymentScheduleRepo.save(schedule);
                    }
                }

            }
        }
    }*/

}
