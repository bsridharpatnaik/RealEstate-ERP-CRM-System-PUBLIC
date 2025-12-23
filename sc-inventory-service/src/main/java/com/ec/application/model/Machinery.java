package com.ec.application.model;

import java.io.Serializable;

import javax.persistence.*;

import com.ec.application.datasync.MultiTableSyncListener;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Data
@Entity
@Table(name = "Machinery")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@EntityListeners(MultiTableSyncListener.class)
public class Machinery extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long machineryId;

    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    @Column(name = "machinery_name", nullable = false)
    String machineryName;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    @Column(name = "machinery_description")
    String machineryDescription;

    public Machinery() {
        machineryName = "";
    }
}
