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

    @Column(name = "firm_address")
    String firmAddress;

    @Column(name = "firm_gst_number")
    String firmGstNumber;

    @Column(name = "firm_pan_number")
    String firmPanNumber;

    @Column(name = "firm_contact_number")
    String firmContactNumber;

    public Firm() {
        firmName = "";
    }
}
