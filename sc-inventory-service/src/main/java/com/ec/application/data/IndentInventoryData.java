package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.util.Date;
import java.util.List;

@Data
public class IndentInventoryData {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @NonNull
    Date indentDate;

    @NonNull
    List<FileInformationDAO> fileInformations;

    @NonNull
    List<IndentProductDTO> inventoryList;
}
