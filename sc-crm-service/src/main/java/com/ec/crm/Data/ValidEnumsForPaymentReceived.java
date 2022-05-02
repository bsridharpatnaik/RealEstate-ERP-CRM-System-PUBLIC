package com.ec.crm.Data;

import com.ec.crm.Enums.PaymentReceivedFromEnum;
import com.ec.crm.Enums.PaymentReceivedPaymentModeEnum;
import lombok.Data;

import java.util.List;

@Data
public class ValidEnumsForPaymentReceived {
    List<String> validPaymentFrom;
    List<String> validMPaymentMode;
}
