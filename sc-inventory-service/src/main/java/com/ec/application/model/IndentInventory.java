package com.ec.application.model;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import com.ec.application.Deserializers.ActiveIndentInventoryListSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity(name = "IndentInventory")
@Table(name = "indent_inventory")
@Audited
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class IndentInventory extends ReusableFields implements Cloneable {

    @Id
    @GeneratedValue(generator = "indent-id-gen")
    @GenericGenerator(
            name = "indent-id-gen",
            strategy = "com.ec.application.IDGenerator.GlobalIndentIdGenerator"
    )
    @Column(name = "indent_id", nullable = false, length = 20)
    private String indentId;

    @Transient
    private String tenantSchemaCode;

    @Column(name="tenant", nullable = false, length = 50)
    String tenant;

    @Column(name = "indent_status", nullable = false, length = 20)
    private String indentStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "indent_date", nullable = false)
    @NonNull
    Date indentDate;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "indent_fileinformation", joinColumns =
            {@JoinColumn(name = "indent_id", referencedColumnName = "indent_id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")})
    Set<FileInformation> fileInformations = new HashSet<>();

    @OneToMany(mappedBy = "indentInventory", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonSerialize(using = ActiveIndentInventoryListSerializer.class)
    private Set<IndentInventoryList> inventoryList = new HashSet<>();

    @Transient
    Boolean approvalAllowed;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}