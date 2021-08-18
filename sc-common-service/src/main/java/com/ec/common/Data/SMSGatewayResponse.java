package com.ec.common.Data;

public class SMSGatewayResponse {
    String message;
    String type;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SMSGatewayResponse{" +
                "message='" + message + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
