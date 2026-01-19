package com.ec.application.data;

import com.ec.application.constants.InwardActionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndentInwardSyncDTO {
    private Date inwardDate;
    private String tenantSchema;
    private Long inwardId;
    private InwardActionType actionType;
    /**
     * One entry per indent line item
     */
    private List<IndentInwardDeltaDTO> deltas;
}