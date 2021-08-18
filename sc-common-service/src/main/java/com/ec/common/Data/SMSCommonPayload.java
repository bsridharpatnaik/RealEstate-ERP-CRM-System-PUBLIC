package com.ec.common.Data;

import lombok.Data;

import java.util.HashMap;
import java.util.List;


public class SMSCommonPayload {
    String flowId;
    List<HashMap<String,String>> smsBody;

    public List<HashMap<String, String>> getSmsBody() {
        return smsBody;
    }

    public void setSmsBody(List<HashMap<String, String>> smsBody) {
        this.smsBody = smsBody;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }
}
