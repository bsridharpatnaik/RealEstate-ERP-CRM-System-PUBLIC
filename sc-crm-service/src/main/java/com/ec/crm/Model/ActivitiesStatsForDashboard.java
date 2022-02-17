package com.ec.crm.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from activities_for_dashboard")
@Immutable
@Data
@NoArgsConstructor
public class ActivitiesStatsForDashboard {
    @Id
    @Column(name = "id")
    String id;

    @Column(name = "type")
    String type;

    @Column(name = "user_name")
    String userName;

    @Column(name = "count")
    Long count;
}
