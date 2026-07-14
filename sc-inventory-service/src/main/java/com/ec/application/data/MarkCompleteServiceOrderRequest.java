package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MarkCompleteServiceOrderRequest {
    /** Optional — set once the vendor confirms when the asset/vendor is next due for servicing. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date nextServiceDate;
    /** Optional per-line warranty — only known once the service is actually performed. */
    private List<LineWarrantyRequest> lineWarranties;
}
