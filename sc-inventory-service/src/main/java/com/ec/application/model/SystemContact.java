package com.ec.application.model;

import javax.persistence.*;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contacts")
@Where(clause = "is_deleted = false AND is_system_contact = true")
@Data
@NoArgsConstructor
@Audited
public class SystemContact extends ContactMappedSuperClass {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "contactId", updatable = false, nullable = false)
    private Long contactId;
}