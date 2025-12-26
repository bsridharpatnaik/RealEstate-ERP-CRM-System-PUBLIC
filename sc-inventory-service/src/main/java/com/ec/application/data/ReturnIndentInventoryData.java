package com.ec.application.data;

import com.ec.application.model.IndentInventory;
import com.ec.application.model.InwardInventory;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class ReturnIndentInventoryData {
    NameAndProjectionDataForDropDown iiDropdown;
    Page<IndentInventory> indentInventories;
}
