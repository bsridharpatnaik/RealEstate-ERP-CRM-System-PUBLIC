package com.ec.crm.Data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeadImportPayloadData {
    String Assignee;
    Long AssigneeId;
    String MobileNo;
    String Name;
}
