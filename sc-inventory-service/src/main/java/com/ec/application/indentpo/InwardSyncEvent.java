package com.ec.application.indentpo;

import com.ec.application.data.IndentInwardSyncDTO;
import org.springframework.context.ApplicationEvent;

public class InwardSyncEvent extends ApplicationEvent {

    private final IndentInwardSyncDTO dto;
    private final String action;

    public InwardSyncEvent(Object source, IndentInwardSyncDTO dto, String action) {
        super(source);
        this.dto = dto;
        this.action = action;
    }

    public IndentInwardSyncDTO getDto() { return dto; }
    public String getAction() { return action; }
}