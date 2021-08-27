package com.ec.application.data;

public class SMSGatewayResponse {
    String message;
    String type;

    public SMSGatewayResponse(String message, String type) {
        this.message=message;
        this.type=type;
    }

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
