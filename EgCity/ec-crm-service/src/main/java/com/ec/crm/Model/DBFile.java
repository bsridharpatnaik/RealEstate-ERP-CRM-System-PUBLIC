package com.ec.crm.Model;

import org.hibernate.annotations.GenericGenerator;

import com.ec.crm.Data.AssigneeDAO;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "files")
@Data
public class DBFile {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String fileName;

    private String fileType;

    @Lob
    private byte[] data;

    
    public DBFile() {

    }

    public DBFile(String fileName, String fileType, byte[] data) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
    }
}
