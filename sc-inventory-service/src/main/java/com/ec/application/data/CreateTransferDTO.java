package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateTransferDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @NonNull
    private Date transferDate;
    private String sourceTenant;
    private String targetTenant;
    private Long sourceWarehouseId;
    private Long targetWarehouseId;
    String remarks;
    List<InventoryTransferItemDTO> items;
}
