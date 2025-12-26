package com.ec.application.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "indent_inventory")
@Audited
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class Indent extends ReusableFields implements Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "indent_id")
    Long indentId;

    @Transient
    private String tenantSchemaCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "indent_date", nullable = false)
    @NonNull
    Date indentDate;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "indent_fileinformation", joinColumns =
            {@JoinColumn(name = "indent_id", referencedColumnName = "indent_id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")})
    Set<FileInformation> fileInformations = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "indentinventory_entry", joinColumns =
            { @JoinColumn(name = "indent_id", referencedColumnName = "indent_id") }, inverseJoinColumns =
            { @JoinColumn(name = "entryid", referencedColumnName = "entryid") })
    Set<IndentInventoryList> inventoryList = new HashSet<>();;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}