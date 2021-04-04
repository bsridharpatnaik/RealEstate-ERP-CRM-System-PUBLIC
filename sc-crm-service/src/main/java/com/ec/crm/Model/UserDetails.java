package com.ec.crm.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from userdetails")
@Immutable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails {
    @Id
    @Column(name="user_id")
    Long userId;

    @Column(name="user_name")
    String userName;

    @Column(name="roles")
    String roles;
}
