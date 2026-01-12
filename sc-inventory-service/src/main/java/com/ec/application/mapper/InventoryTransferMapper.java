package com.ec.application.mapper;

import com.ec.application.data.InventoryTransferDTO;
import com.ec.application.data.InventoryTransferItemDTO;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.model.InventoryTransferItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryTransferMapper {

    InventoryTransferDTO toDto(InventoryTransfer transfer);

    InventoryTransferItemDTO toItemDto(InventoryTransferItem item);

    List<InventoryTransferItemDTO> toItemDtoList(List<InventoryTransferItem> items);
}