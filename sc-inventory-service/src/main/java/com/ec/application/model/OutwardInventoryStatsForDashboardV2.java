package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from outward_stats")
@Immutable
@Audited
@Data
public class OutwardInventoryStatsForDashboardV2 {
    @Id
    @Column(name="contractor_name")
    String contractorName;

    @Column(name="total_count")
    String totalCount;

    @Column(name="reject_count")
    String rejectCount;
}
