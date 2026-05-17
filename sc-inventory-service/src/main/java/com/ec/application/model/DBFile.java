package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

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
    private String bucketName;

    public DBFile() {}

    public DBFile(String fileName, String fileType, String bucketName) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.bucketName = bucketName;
    }
}
