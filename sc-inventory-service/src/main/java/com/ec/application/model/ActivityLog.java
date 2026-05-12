package com.ec.application.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_al_time", columnList = "activity_time"),
        @Index(name = "idx_al_entity", columnList = "entity_type,entity_id")
})
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "activity_time", nullable = false)
    private Date activityTime;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getActivityTime() { return activityTime; }
    public void setActivityTime(Date activityTime) { this.activityTime = activityTime; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
}
