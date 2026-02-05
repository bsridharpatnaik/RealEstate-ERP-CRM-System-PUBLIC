package com.ec.application.data;

import java.util.Date;

public class PoStatusChangeDTO {

    private String newStatus;
    private Date changedAt;

    public PoStatusChangeDTO(String newStatus, Date changedAt) {
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public Date getChangedAt() {
        return changedAt;
    }
}

