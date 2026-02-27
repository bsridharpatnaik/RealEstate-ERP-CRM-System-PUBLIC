package com.ec.application.model;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.datasync.MultiTableSyncListener;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "Firm")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@EntityListeners(MultiTableSyncListener.class)
public class Firm extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long firmId;

    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    @Column(name = "firm_name", nullable = false)
    String firmName;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    @Column(name = "firm_description")
    String firmDescription;

    @Column(name = "firm_gst_number")
    String firmGstNumber;

    @Column(name = "firm_pan_number")
    String firmPanNumber;

    @Column(name = "firm_email")
    String firmEmail;

    @Column(name = "contactPerson", nullable = true, length = 255)
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String contactPerson;

    @Column(name = "contactPersonMobileNo", nullable = true, length = 255)
    private String contactPersonMobileNo;

    @Column(name = "addr_line1", nullable = true, length = 255)
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String addr_line1;

    @Column(name = "addr_line2", nullable = true, length = 255)
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String addr_line2;

    @Column(name = "city", nullable = true, length = 255)
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String city;

    @Column(name = "state", nullable = true, length = 255)
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    private String state;

    @Column(name = "zip", nullable = true, length = 255)
    private String zip;

    @Column(name = "firm_contact_number", nullable = false)
    private String firmContactNumber;   // e.g. "9876543210,9123456789"

    public Firm() {
        firmName = "";
    }
}
