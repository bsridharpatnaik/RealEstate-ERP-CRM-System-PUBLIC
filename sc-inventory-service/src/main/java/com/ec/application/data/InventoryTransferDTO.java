package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InventoryTransferDTO {

    private Long transferId;
    private String sourceTenant;
    private String targetTenant;
    private Long sourceWarehouseId;
    private Long targetWarehouseId;
    private Date transferDate;

    private List<InventoryTransferItemDTO> items;
}