package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.sql.Date;

@Data
@AllArgsConstructor
public class IMPPDeleteDTO {

    @JsonFormat(pattern = "yyyy-MM")
    @NonNull
    Date date;

    @NonNull
    Long productId;
}
