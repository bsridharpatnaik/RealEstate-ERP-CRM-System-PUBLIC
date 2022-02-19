package com.ec.crm.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from lead_stage_agent_mapping")
@Immutable
@Data
@NoArgsConstructor
public class LeadStageAgentMapping {
    @Id
    @Column(name = "user_name")
    String userName;

    @Column(name = "new_lead")
    Long newLead;

    @Column(name = "visit_scheduled")
    Long visitScheduled;

    @Column(name = "visit_completed")
    Long visitCompleted;

    @Column(name = "negotiation")
    Long negotiation;

    @Column(name = "deal_lost")
    Long dealLost;

    @Column(name = "deal_closed")
    Long dealClosed;
}
