package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_draft")
@Getter
@Setter
@NoArgsConstructor
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class Draft extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long draftId;

    @Column(name = "tenant", length = 50, nullable = true)
    private String tenant;

    @Column(name = "draft_type", length = 50, nullable = false)
    private String draftType;

    @Column(name = "draft_name", length = 150)
    private String draftName;

    @Column(name = "username", length = 50, nullable = false)
    String username;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    public Draft(String draftType, String payload, String username) {
        this.draftType = draftType;
        this.payload = payload;
        this.username = username;
    }
}