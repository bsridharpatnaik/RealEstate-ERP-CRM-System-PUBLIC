package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryRelationInput {

    private String relationType;   // HistoryRelationType.PO / INWARD / INDENT
    private String tenant;
    private String referenceId;    // PO-27, INW-118, IN-223
}

