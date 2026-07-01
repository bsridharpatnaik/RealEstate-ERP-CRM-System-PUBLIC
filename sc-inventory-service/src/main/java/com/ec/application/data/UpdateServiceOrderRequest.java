package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
public class UpdateServiceOrderRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date serviceDate;
    private Long vendorId;
    private Long firmId;
    private String subject;
    private String notes;
    private Double grandTotal;
    @Column(length = 35)
    private String overridePhoneNumber;
    private String overrideEmail;
    private String projectName;
    private Double specialDiscount;
    private List<FileInformationDAO> fileInformations;
    /** Edits to existing lines (matched by lineId) */
    private List<UpdateServiceOrderLineRequest> lineUpdates;
    /** Brand-new lines to append (no lineId) */
    private List<CreateServiceOrderLineRequest> newLines;
    /** IDs of existing lines to remove */
    private List<Long> removedLineIds;
}
