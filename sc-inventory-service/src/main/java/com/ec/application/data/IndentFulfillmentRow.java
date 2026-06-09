package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndentFulfillmentRow {

    private String project;
    private String indentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private Date indentDate;

    private String indentStatus;
    private String productName;
    private String productCode;
    private String unit;

    private Double requestedQty;
    private Double receivedQty;
    private Double pendingQty;
    private Double percentFulfilled;

    private String poNumber;
    private String lineItemStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private Date needByDate;
}
