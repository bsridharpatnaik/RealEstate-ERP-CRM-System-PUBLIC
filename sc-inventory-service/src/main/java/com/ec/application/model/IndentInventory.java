package com.ec.application.model;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.ec.application.Deserializers.ActiveIndentInventoryListSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
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

    /**
     * Effective expected date — computed as the minimum needByDate across all active line items.
     * Not persisted in the header table; populated at query time by IndentInventoryUiEnricher.
     */
    @Transient
    @JsonProperty("needByDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date needByDate;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "indent_fileinformation", joinColumns =
            {@JoinColumn(name = "indent_id", referencedColumnName = "indent_id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")})
    Set<FileInformation> fileInformations = new HashSet<>();

    @OneToMany(mappedBy = "indentInventory", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonSerialize(using = ActiveIndentInventoryListSerializer.class)
    @OrderBy("entryid ASC")
    private Set<IndentInventoryList> inventoryList = new HashSet<>();

    @OneToMany(
            mappedBy = "indent",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    @JsonIgnore   // optional – depends if you want it in API response
    private List<IndentStatusHistory> statusHistory = new ArrayList<>();

    @Transient
    Boolean approvalAllowed;

    @Transient
    private List<String> poNumbers;

    @Column(
            name = "last_status_updated_at",
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d MMM yyyy h:mm a")
    private Date lastStatusUpdatedAt;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}