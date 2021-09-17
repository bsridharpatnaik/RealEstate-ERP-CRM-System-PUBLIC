package com.ec.crm.Model;

import com.ec.crm.ReusableClasses.ReusableFields;
import lombok.Data;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "user_mobile_mapping")
@Data
public class UserMobileNoMapping {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_Id", updatable = false, nullable = false)
    Long userId;

    @Column(name="mobile_number")
    String mobileNumber;
}
