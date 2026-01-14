package com.ec.application.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@NoArgsConstructor
public class FileInformationDAO {
    @NonNull
    String fileUUId;

    @NonNull
    String fileName;
}
