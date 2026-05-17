package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
// creationDate comes from ReusableFields (@CreatedDate → "creationDate" column)

@Data
@AllArgsConstructor
public class DraftSummary {
    private Long draftId;
    private String draftName;
    private Date createdDate;
}
