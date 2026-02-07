package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class StatusGroupCountDTO {

    private String status;
    private String groupKey;   // tenant OR firm
    private Long count;

    public StatusGroupCountDTO(String status, String groupKey, Long count) {
        this.status = status;
        this.groupKey = groupKey;
        this.count = count;
    }
}
