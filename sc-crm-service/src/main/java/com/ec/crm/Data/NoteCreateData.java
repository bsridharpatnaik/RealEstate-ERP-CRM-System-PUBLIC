package com.ec.crm.Data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteCreateData {

    String content;

    @NonNull
    Long leadId;

    Boolean pinned;

    @NonNull
    List<FileInformationDAO> fileInformations;

    public NoteCreateData(Long leadId, String s) {
        this.leadId = leadId;
        this.content = s;
        this.fileInformations = new ArrayList<FileInformationDAO>();
        this.pinned = false;
    }

    @Override
    public String toString() {
        return "NoteCreateData [content=" + content + ", leadId=" + leadId + ", pinned=" + pinned
                + ", fileInformations=" + fileInformations + "]";
    }
}
