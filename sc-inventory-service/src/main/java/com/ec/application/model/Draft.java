package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_draft")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class Draft extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long draftId;

    @Column(name = "draft_type", length = 50, nullable = false)
    private String draftType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    public Draft(String draftType, String payload) {
        this.draftType = draftType;
        this.payload = payload;
    }
}