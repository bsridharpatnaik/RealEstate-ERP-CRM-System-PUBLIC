package com.ec.crm.Data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadImportResponseData {
    String Assignee;
    String MobileNo;
    String Name;
    String result;
}
