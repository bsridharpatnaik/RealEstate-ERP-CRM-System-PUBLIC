package com.ec.crm.Data;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class SMSExternalPayloadDataForSingle {

    HashMap<String,String> payload;

    public SMSExternalPayloadDataForSingle(HashMap<String, String> payload) {
        this.payload = payload;
    }
}
