package com.ec.application.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SMSExternalPayloadData {
    String flow_id;
    List<HashMap<String,String>> recipients;

    public SMSExternalPayloadData(String flow_id, List<HashMap<String, String>> recipients) {
        this.flow_id=flow_id;
        this.recipients = recipients;
    }

    public String getFlow_id() {
        return flow_id;
    }

    public void setFlow_id(String flow_id) {
        this.flow_id = flow_id;
    }

    public List<HashMap<String, String>> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<HashMap<String, String>> recipients) {
        this.recipients = recipients;
    }

    @Override
    public String toString() {
        return "SMSExternalPayloadData{" +
                "flow_id='" + flow_id + '\'' +
                ", recipients=" + recipients +
                '}';
    }
}
