package com.ec.crm.Data;

import lombok.Data;

import java.util.List;

@Data
public class BulkLeadImport {
    String importType;
    List<LeadImportPayloadData> data;
}
