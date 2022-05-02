package com.ec.crm.Controller;

import com.ec.crm.Data.DealStructurePaymentReceivedDTO;
import com.ec.crm.Data.PaymentReceivedCreateData;
import com.ec.crm.Data.PaymentScheduleListingDTO;
import com.ec.crm.Data.ValidEnumsForPaymentReceived;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.PaymentReceived;
import com.ec.crm.Service.PaymentReceivedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/customer/payments/received")
public class PaymentReceivedController {

    @Autowired
    PaymentReceivedService paymentReceivedService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentReceived createNewPayment(@RequestBody PaymentReceivedCreateData payload) throws Exception {
        return paymentReceivedService.createNewPayment(payload);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentReceived updatePayment(@RequestBody PaymentReceivedCreateData payload) throws Exception {
        return paymentReceivedService.updatePayment(payload);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> updatePayment(@PathVariable Long paymentId) throws Exception {
        paymentReceivedService.deletePayment(paymentId);
        return ResponseEntity.ok("Entity deleted");
    }

    @GetMapping("/{dealId}/paymentlist")
    @ResponseStatus(HttpStatus.OK)
    public DealStructurePaymentReceivedDTO getPaymentList(@PathVariable Long dealId) throws Exception {
        return paymentReceivedService.getPaymentsList(dealId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ValidEnumsForPaymentReceived getValidEnumForDropdown() {
        return paymentReceivedService.getValidEnumForDropdown();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) ->
        {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
