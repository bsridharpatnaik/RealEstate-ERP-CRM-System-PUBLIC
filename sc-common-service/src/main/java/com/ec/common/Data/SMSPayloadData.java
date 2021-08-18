package com.ec.common.Data;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

public class SMSPayloadData {
    String flow_id;
    String sender;
    List<HashMap<String,String>> recipients;

    public SMSPayloadData(String flow_id, String sender, List<HashMap<String, String>> recipients) {
        this.flow_id=flow_id;
        this.sender=sender;
        this.recipients=recipients;
    }

    public String getFlow_id() {
        return flow_id;
    }

    public void setFlow_id(String flow_id) {
        this.flow_id = flow_id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public List<HashMap<String, String>> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<HashMap<String, String>> recipients) {
        this.recipients = recipients;
    }
}
