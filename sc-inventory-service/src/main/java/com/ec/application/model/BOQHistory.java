package com.ec.application.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "boq_history")
public class BOQHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "changeDateTime")
    private Date changeDateTime;

    @Column(name = "changeType")
    private String changeType;

    @Column(name = "changedBy")
    private String changedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buildingTypeId")
    private BuildingType buildingType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usageLocationId")
    private UsageLocation usageLocation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "productId")
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "finalLocationId")
    private UsageArea finalLocation;

    @Column(name = "oldQuantity")
    private Double oldQuantity;

    @Column(name = "newQuantity")
    private Double newQuantity;

    @Column(name = "remark", length = 500)
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getChangeDateTime() { return changeDateTime; }
    public void setChangeDateTime(Date changeDateTime) { this.changeDateTime = changeDateTime; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public BuildingType getBuildingType() { return buildingType; }
    public void setBuildingType(BuildingType buildingType) { this.buildingType = buildingType; }

    public UsageLocation getUsageLocation() { return usageLocation; }
    public void setUsageLocation(UsageLocation usageLocation) { this.usageLocation = usageLocation; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public UsageArea getFinalLocation() { return finalLocation; }
    public void setFinalLocation(UsageArea finalLocation) { this.finalLocation = finalLocation; }

    public Double getOldQuantity() { return oldQuantity; }
    public void setOldQuantity(Double oldQuantity) { this.oldQuantity = oldQuantity; }

    public Double getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Double newQuantity) { this.newQuantity = newQuantity; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
